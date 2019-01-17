package org.renci.mobius.controllers;

import org.renci.mobius.notification.NotificationPublisher;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class Workflow {
    private String workflowID;
    protected WorkflowOperationLock lock;
    private HashMap<String, CloudContext> siteToContextHashMap;
    private int nodeCount;
    private FutureRequests futureRequests;

    Workflow(String id) {
        workflowID = id;
        lock = new WorkflowOperationLock();
        siteToContextHashMap = new HashMap<String, CloudContext>();
        nodeCount = 0;
        futureRequests = new FutureRequests();
    }

    public String getWorkflowID() {
        return workflowID;
    }

    public void stop() throws Exception {
        CloudContext context = null;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            context.stop();
        }
        siteToContextHashMap.clear();
    }

    public String status() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        CloudContext context = null;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            stringBuilder.append(context.getStatus());
        }
        return stringBuilder.toString();
    }
    public void lock() throws InterruptedException {
        lock.acquire();
    }

    public void unlock() {
        lock.release();
    }

    public boolean locked() {
        return (lock.availablePermits() == 0);
    }

    public void processComputeRequest(ComputeRequest request) throws Exception{
        try {
            // Lookup an existing stack
            CloudContext s = siteToContextHashMap.get(request.getSite());
            boolean addContextToMap = false;

            // Create a new slice if not found
            if (s == null) {
                s = CloudContextFactory.getInstance().createCloudContext(request.getSite());
                addContextToMap = true;
            }

            int count = s.processCompute(workflowID, request, nodeCount);
            nodeCount += count;
            if (addContextToMap) {
                siteToContextHashMap.put(request.getSite(), s);
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
    }

    public void doPeriodic() {
        StringBuilder stringBuilder = new StringBuilder();
        CloudContext context = null;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            String result = context.doPeriodic();
            if(result != null) {
                stringBuilder.append(result);
            }
        }
        String notification = stringBuilder.toString();
        if(!notification.isEmpty()) {
            // TODO send notification to Pegaus
            System.out.println("Sending notification to Pegasus = " + notification);
            if(NotificationPublisher.getInstance().isConnected()) {
                NotificationPublisher.getInstance().push(workflowID, notification);
            }
        }
        processFutureComputeRequests();
        processFutureStorageRequests();
    }

    public void processStorageRequest(StorageRequest request) throws Exception{
        System.out.println("Processing future storage requests");
        try {
            if (siteToContextHashMap.size() == 0) {
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
            CloudContext context = null;
            for (HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
                context = e.getValue();
                if (context.containsHost(request.getTarget())) {
                    context.processStorageRequest(request);
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
                }
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
    }

    public void processFutureComputeRequests() {
        System.out.println("Processing future compute requests");
        try {
            List<ComputeRequest> computeRequests = futureRequests.getFutureComputeRequests();
            Iterator iterator = computeRequests.iterator();
            while (iterator.hasNext()) {
                ComputeRequest request = (ComputeRequest) iterator.next();
                try {
                    processComputeRequest(request);
                }
                catch (FutureRequestException e)
                {
                    System.out.println("future request");
                }
                catch (Exception e) {
                    System.out.println("Error occurred while processing future compute request = " + e.getMessage());
                }
                futureRequests.remove(request);

            }
        }
        catch (Exception e) {
            System.out.println("Error occurred while processing future compute request = " + e.getMessage());
        }
    }

    public void processFutureStorageRequests() {
        try {
            List<StorageRequest> storageRequests = futureRequests.getFutureStorageRequests();
            Iterator iterator = storageRequests.iterator();
            while (iterator.hasNext()) {
                StorageRequest request = (StorageRequest) iterator.next();
                try {
                    processStorageRequest(request);
                }
                catch (FutureRequestException e)
                {
                    System.out.println("future request");
                }
                catch (Exception e) {
                    System.out.println("Error occurred while processing future compute request = " + e.getMessage());
                }
                futureRequests.remove(request);

            }
        }
        catch (Exception e) {
            System.out.println("Error occurred while processing future compute request = " + e.getMessage());
        }
    }
}
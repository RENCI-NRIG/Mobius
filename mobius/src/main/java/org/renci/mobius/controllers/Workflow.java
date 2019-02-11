package org.renci.mobius.controllers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.notification.NotificationPublisher;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

class Workflow {
    private String workflowID;
    protected WorkflowOperationLock lock;
    private HashMap<String, CloudContext> siteToContextHashMap;
    private int nodeCount, storageCount;
    private FutureRequests futureRequests;
    private static final Logger LOGGER = Logger.getLogger( Workflow.class.getName() );

    Workflow(String id) {
        workflowID = id;
        lock = new WorkflowOperationLock();
        siteToContextHashMap = new HashMap<String, CloudContext>();
        nodeCount = 0;
        storageCount = 0;
        futureRequests = new FutureRequests();
    }

    public String getWorkflowID() {
        return workflowID;
    }

    public void stop() throws Exception {
        LOGGER.debug("stop(): IN");
        CloudContext context = null;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            context.stop();
        }
        siteToContextHashMap.clear();
        LOGGER.debug("stop(): OUT");
    }

    public String status() throws Exception {
        LOGGER.debug("status(): IN");

        JSONArray array = new JSONArray();

        CloudContext context = null;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            JSONObject result = context.getStatus();
            if(result != null && !result.isEmpty()) {
                array.add(result);
            }
        }
        LOGGER.debug("status(): OUT");
        return array.toString();
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

    public void processComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception{
        LOGGER.debug("processComputeRequest(): IN");

        try {
            if(request.getSlicePolicy() == ComputeRequest.SlicePolicyEnum.EXISTING && request.getSliceName() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Slice name must be specified for SlicePolicy-exisiting");
            }

            CloudContext context = null;
            boolean addContextToMap = false;
            if(request.getSlicePolicy() == ComputeRequest.SlicePolicyEnum.EXISTING) {
                // Look up existing slice
                for(CloudContext c : siteToContextHashMap.values()) {
                    if(c.containsSlice(request.getSliceName())) {
                        context = c;
                        break;
                    }
                }
                if(context == null) {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Slice not found for SlicePolicy-exisiting");
                }
            }
            else {
                // Lookup an existing stack
                context = siteToContextHashMap.get(request.getSite());
                // Create a new slice if not found
                if (context == null) {
                    context = CloudContextFactory.getInstance().createCloudContext(request.getSite());
                    addContextToMap = true;
                }
            }

            nodeCount = context.processCompute(workflowID, request, nodeCount, isFutureRequest);
            if (addContextToMap) {
                siteToContextHashMap.put(request.getSite(), context);
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
        LOGGER.debug("processComputeRequest(): OUT");
    }
    public void processStorageRequest(StorageRequest request, boolean isFutureRequest) throws Exception{
        LOGGER.debug("processStorageRequest(): IN");
        try {
            if (siteToContextHashMap.size() == 0) {
                LOGGER.debug("processStorageRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
            CloudContext context = null;
            for (HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
                context = e.getValue();
                if (context.containsHost(request.getTarget())) {
                    LOGGER.debug("Context found to handle storage request=" + context.getSite());
                    storageCount = context.processStorageRequest(request, storageCount, isFutureRequest);
                    break;
                }else {
                    context = null;
                }
            }
            if(context == null) {
                LOGGER.debug("processStorageRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
        finally {
            LOGGER.debug("processStorageRequest(): OUT");
        }
    }

    public void doPeriodic() {
        LOGGER.debug("doPeriodic(): IN");

        JSONArray array = new JSONArray();
        CloudContext context = null;
        boolean triggerNotification = false;
        for(HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
            context = e.getValue();
            JSONObject result = context.doPeriodic();
            if(result != null && !result.isEmpty()) {
                array.add(result);
            }
            triggerNotification |= context.isTriggerNotification();
            if(context.isTriggerNotification()) {
                context.setTriggerNotification(false);
            }
        }
        String notification = array.toString();
        if(triggerNotification && !notification.isEmpty()) {
            if(NotificationPublisher.getInstance().isConnected()) {
                LOGGER.debug("Sending notification to Pegasus = " + notification);
                NotificationPublisher.getInstance().push(workflowID, notification);
            }
            else {
                LOGGER.debug("Unable to send notification to Pegasus = " + notification);
            }
        }
        // Process future requests
        processFutureComputeRequests();
        processFutureStorageRequests();
        LOGGER.debug("doPeriodic(): OUT");
    }

    public void processFutureComputeRequests() {
        LOGGER.debug("processFutureComputeRequests(): IN");
        try {
            List<ComputeRequest> computeRequests = futureRequests.getFutureComputeRequests();
            Iterator iterator = computeRequests.iterator();
            while (iterator.hasNext()) {
                ComputeRequest request = (ComputeRequest) iterator.next();
                try {
                    processComputeRequest(request, true);
                }
                catch (FutureRequestException e)
                {
                    LOGGER.debug("future request");
                }
                catch (Exception e) {
                    LOGGER.error("Error occurred while processing future compute request = " + e.getMessage());
                    e.printStackTrace();
                }
                finally {
                    futureRequests.remove(request);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Error occurred while processing future compute request = " + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.debug("processFutureComputeRequests(): OUT");
    }

    public void processFutureStorageRequests() {
        LOGGER.debug("processFutureStorageRequests(): IN");

        try {
            List<StorageRequest> storageRequests = futureRequests.getFutureStorageRequests();
            Iterator iterator = storageRequests.iterator();
            while (iterator.hasNext()) {
                StorageRequest request = (StorageRequest) iterator.next();
                try {
                    processStorageRequest(request, true);
                }
                catch (FutureRequestException e)
                {
                    LOGGER.debug("future request");
                }
                catch (Exception e) {
                    LOGGER.error("Error occurred while processing future compute request = " + e.getMessage());
                    e.printStackTrace();
                }
                finally {
                    futureRequests.remove(request);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("Error occurred while processing future compute request = " + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.debug("processFutureStorageRequests(): OUT");
    }
}
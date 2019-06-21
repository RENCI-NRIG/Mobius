package org.renci.mobius.controllers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.renci.mobius.entity.WorkflowEntity;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.notification.NotificationPublisher;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import javax.swing.*;

/*
 * @brief class represents a worflow and maintains hashmap of cloud context per site
 *
 * @author kthare10
 */
class Workflow {
    private String workflowID;
    protected WorkflowOperationLock lock;
    private HashMap<String, CloudContext> siteToContextHashMap;
    private int nodeCount, storageCount, stitchCount;
    private FutureRequests futureRequests;
    private static final Logger LOGGER = Logger.getLogger( Workflow.class.getName() );

    /*
     * @brief constructor
     *
     * @param id - workflow id
     */
    Workflow(String id) {
        workflowID = id;
        lock = new WorkflowOperationLock();
        siteToContextHashMap = new HashMap<String, CloudContext>();
        nodeCount = 0;
        storageCount = 0;
        stitchCount = 0;
        futureRequests = new FutureRequests();
    }

    /*
     * @brief constructor
     *
     * @param workflow - workflow entity read from database
     */
    Workflow(WorkflowEntity workflow) {
        workflowID = workflow.getWorkflowId();
        LOGGER.debug("Workflow(): workflowID=" + workflowID);
        nodeCount = workflow.getNodeCount();
        LOGGER.debug("Workflow(): nodeCount=" + nodeCount);
        storageCount = workflow.getStorageCount();
        LOGGER.debug("Workflow(): storageCount=" + storageCount);
        stitchCount = workflow.getStorageCount();
        LOGGER.debug("Workflow(): stitchCount=" + stitchCount);
        lock = new WorkflowOperationLock();
        siteToContextHashMap = new HashMap<String, CloudContext>();
        futureRequests = new FutureRequests();

        // TODO process json to construct siteToContextHashMap
        if(workflow.getSiteContextJson() != null) {
            LOGGER.debug("SiteContext =" + workflow.getSiteContextJson());
            JSONArray array = (JSONArray) JSONValue.parse(workflow.getSiteContextJson());
            if(array != null) {
                for (Object object : array) {
                    try {
                        JSONObject c = (JSONObject) object;
                        String site = (String) c.get("site");
                        LOGGER.debug("Workflow(): site=" + site);
                        CloudContext context = CloudContextFactory.getInstance().createCloudContext(site, workflow.getWorkflowId());
                        JSONArray sliceArray = (JSONArray) c.get("slices");
                        context.fromJson(sliceArray);
                        context.loadCloudSpecificDataFromJson(c);
                        siteToContextHashMap.put(site, context);
                    } catch (Exception e) {
                        LOGGER.error("Workflow(): Exception occured while loading context from database e= " + e);
                        e.printStackTrace();
                    }
                }
            }
            else {
                LOGGER.error("Workflow(): JSON parsing failed");
            }
        }
    }

    /*
     * @brief returns workflow id
     *
     * @return worlflow id
     */
    public String getWorkflowID() {
        return workflowID;
    }

    /*
     * @brief converts workflow object into workflow entity to be written to database
     *
     * @return workflow entity
     */
    public WorkflowEntity convert() {
        WorkflowEntity retVal = new WorkflowEntity(this.workflowID, nodeCount, storageCount,
                stitchCount, null);

        if(siteToContextHashMap != null && siteToContextHashMap.size() != 0) {
            JSONArray array = new JSONArray();
            CloudContext context = null;
            for (HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
                context = e.getValue();
                JSONObject c = new JSONObject();
                c.put("type", context.getCloudType().toString());
                c.put("site", context.getSite());
                JSONArray slices = context.toJson();
                if(slices != null) {
                    c.put("slices", slices);
                }
                c = context.addCloudSpecificDataToJson(c);
                array.add(c);
            }
            LOGGER.debug("convert(): array=" + array.toJSONString());
            retVal = new WorkflowEntity(this.workflowID, nodeCount, storageCount, stitchCount, array.toJSONString());
        }

        return retVal;
    }


    /*
     * @brief function to release all resources associated with this workflow
     */
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

    /*
     * @brief function to check get status for the workflow
     *
     * @return string representing status
     */
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

    /*
     * @brief acquire mutex lock
     *
     * @throws InterruptedException
     */
    public void lock() throws InterruptedException {
        lock.acquire();
    }

    /*
     * @brief release mutex lock
     *
     */
    public void unlock() {
        lock.release();
    }

    /*
     * @brief true of mutex is lock; false otherwise
     *
     * @return true of mutex is lock; false otherwise
     */
    public boolean locked() {
        return (lock.availablePermits() == 0);
    }

    /*
     * @brief function to process compute request
     *
     * @param request - compute request
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     */
    public void processComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception{
        LOGGER.debug("processComputeRequest(): IN");
        CloudContext context = null;
        boolean addContextToMap = false;
        try {
            if(request.getSlicePolicy() == ComputeRequest.SlicePolicyEnum.EXISTING && request.getSliceName() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Slice name must be specified for SlicePolicy-exisiting");
            }

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
                    context = CloudContextFactory.getInstance().createCloudContext(request.getSite(), workflowID);
                    addContextToMap = true;
                }
            }

            Pair<Integer, Integer> r = context.processCompute(request, nodeCount, stitchCount, isFutureRequest);
            nodeCount = r.getFirst();
            stitchCount = r.getSecond();
            LOGGER.debug("processComputeRequest(): nodeCount = " + nodeCount);
            if (addContextToMap) {
                siteToContextHashMap.put(request.getSite(), context);
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
        catch (Exception e) {
            // New context was created but compute request failed to process;
            // context is not saved in this case and hence should release any open resources
            // e.g. network created for chameleon
            if(context != null && addContextToMap) {
                context.stop();
            }
            throw e;
        }
        LOGGER.debug("processComputeRequest(): OUT");
    }

    /*
     * @brief function to process storge request
     *
     * @param request - storge request
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     */
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

    /*
     * @brief function to process network request
     *
     * @param request - network request
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequest(NetworkRequest request, boolean isFutureRequest) throws Exception{
        LOGGER.debug("processNetworkRequest(): IN");
        try {
            if (siteToContextHashMap.size() == 0) {
                LOGGER.debug("processNetworkRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
            CloudContext sourceContext = null, targetContext = null;
            for (HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
                sourceContext = e.getValue();
                targetContext = e.getValue();
                if (sourceContext == null && e.getValue().containsHost(request.getSource())) {
                    sourceContext = e.getValue();
                    LOGGER.debug("Context found to handle network request=" + sourceContext.getSite());
                }
                if (targetContext == null && e.getValue().containsHost(request.getDestination())) {
                    targetContext = e.getValue();
                    LOGGER.debug("Context found to handle network request=" + targetContext.getSite());
                }
                if(sourceContext != null && targetContext != null) {
                    break;
                }
            }
            if(sourceContext == null || targetContext == null) {
                LOGGER.debug("processNetworkRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, " source or destination context not found");
            }

            // sudo ${BIN_DIR}/SafeSdxExogeniClient -c client-config/c0.conf -e 'stitch client-1 192.168.20.2 192.168.20.1/24'
            JSONObject object =  new JSONObject();
            // Exogeni User PEM
            object.put("config.exogenipem", MobiusConfig.getInstance().getDefaultExogeniUserCertKey());
            // Exogeni Controller URL
            object.put("config.exogenism", MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
            // Exogeni Slice name
            object.put("config.slicename", sourceContext.getSite());

            LOGGER.debug("processNetworkRequest(): source = " + object.toString());

            // Exogeni Slice name
            object.put("config.slicename", targetContext.getSite());

            LOGGER.debug("processNetworkRequest(): source = " + object.toString());
        }
        finally {
            LOGGER.debug("processNetworkRequest(): OUT");
        }
    }
    /*
     * @brief function to process a stitch request;
     *
     * @param request - stitch request
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     *
     */
    public void processStitchRequest(StitchRequest request, boolean isFutureRequest) throws Exception{
        LOGGER.debug("processStitchRequest(): IN");
        try {
            if (siteToContextHashMap.size() == 0) {
                LOGGER.debug("processStitchRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
            CloudContext context = null;
            for (HashMap.Entry<String, CloudContext> e : siteToContextHashMap.entrySet()) {
                context = e.getValue();
                if (context.containsHost(request.getTarget())) {
                    LOGGER.debug("Context found to handle storage request=" + context.getSite());
                    stitchCount = context.processStitchRequest(request, stitchCount, isFutureRequest);
                    break;
                }else {
                    context = null;
                }
            }
            if(context == null) {
                LOGGER.debug("processStitchRequest(): OUT");
                throw new MobiusException(HttpStatus.NOT_FOUND, "target not found");
            }
        }
        catch (FutureRequestException e) {
            futureRequests.add(request);
        }
        finally {
            LOGGER.debug("processStitchRequest(): OUT");
        }
    }

    /*
     * @brief performs following periodic actions
     *        - Reload hostnames of all instances
     *        - Reload hostNameToSliceNameHashMap
     *        - Determine if notification to pegasus should be triggered
     *        - Build notification JSON object
     *        - process future requests
     *
     * @return JSONObject representing notification for context to be sent to pegasus
     */
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

    /*
     * @brief check and trigger any future compute requests if their startTime is current time
     */
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

    /*
     * @brief check and trigger any future storage requests if their startTime is current time
     */
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
package org.renci.mobius.controllers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.comet.CometDataManager;
import org.renci.mobius.entity.WorkflowEntity;
import org.renci.mobius.model.*;
import org.renci.mobius.service.WorkflowService;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/*
 * @brief class implements singleton main entry point into Mobius business logic;
 *        - maintains all workflows in a hashmap;
 *        - maintains all workflows in database
 *        - performs periodic operations
 *
 * @author kthare10
 */
public class MobiusController {

    private static final MobiusController fINSTANCE = new MobiusController();
    private static final Logger LOGGER = LogManager.getLogger( MobiusController.class.getName() );

    // thread for syncing tags from existing reservations
    protected static PeriodicProcessingThread ppt = null;
    protected static ScheduledFuture<?> pptFuture = null;

    private HashMap<String, Workflow> workflowHashMap;
    private WorkflowService service;

    /*
     * @brief constructor
     */
    private MobiusController() {
        workflowHashMap = new HashMap<String, Workflow>();
        service = null;
    }

    /*
     * @brief returns factory instance
     *
     * @return factory instance
     */
    public static MobiusController getInstance() {
        return fINSTANCE;
    }

    /*
     * @brief set service object
     *
     * @param service - service object
     */
    public void setService(WorkflowService service) {
        synchronized (this) {
            this.service = service;
        }
    }

    enum DbOperation {
        Create,
        Update,
        Delete

    };

    /*
     * @brief function which writes context per workflow to databae
     *
     * @param workflow - workflow to be written
     * @param operation - operation to be performed
     */
    private void dbWrite(Workflow workflow, DbOperation operation) {
        try {
            if (workflow != null) {
                switch (operation) {
                    case Create:
                        if (service != null) {
                            WorkflowEntity workflowEntity = workflow.convert();
                            service.createWorkflow(workflowEntity);
                        }
                        break;
                    case Delete:
                        if (service != null) {
                            service.deleteWorkflow(workflow.getWorkflowID());
                        }
                        break;
                    case Update:
                        if (service != null) {
                            WorkflowEntity workflowEntity = workflow.convert();
                            service.editWorkflow(workflowEntity);
                        }
                        break;
                }
            } else {
                LOGGER.error("workflow is null");
            }
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred e=" + e);
            e.printStackTrace();
            throw e;
        }

    }

    /*
     * @brief function responsible to return list of the workflows
     *
     *
     * @throws exception in case of error
     */
    public JSONObject listWorkflows() throws Exception{
        LOGGER.debug("IN");
        if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
            LOGGER.debug("OUT");
            throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
        }
        try {

            JSONObject result = new JSONObject();
            synchronized (this) {
                JSONArray workflows = new JSONArray();
                for(String workflowId : workflowHashMap.keySet()) {
                    workflows.add(workflowId);
                }
                result.put("workflows", workflows);
            }
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new MobiusException("Internal Server Error e=" + e);
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to create a worflow if one does not exist
     *
     * @param workflowID - worklfow id
     *
     * @throws exception in case of error
     */
    public void createWorkflow(String workflowID) throws Exception{
        LOGGER.debug("IN workflowID=" + workflowID);
        if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
            LOGGER.debug("OUT");
            throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
        }
        try {
            Workflow workflow = null;
            synchronized (this) {
                workflow = workflowHashMap.get(workflowID);
                if( workflow != null) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Workflow Id already in use");
                }
                workflow = new Workflow(workflowID);
                workflowHashMap.put(workflowID, workflow);
                dbWrite(workflow, DbOperation.Create);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new MobiusException("Internal Server Error e=" + e);
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to delete a worflow if one exist
     *
     * @param workflowID - worklfow id
     *
     */
    public void deleteWorkflow(Workflow workflow) {
        LOGGER.debug("IN workflow=" + workflow);
        if(workflow != null) {
            synchronized (this) {
                workflowHashMap.remove(workflow.getWorkflowID());
            }
        }
        LOGGER.debug("OUT");
    }

    /*
     * @brief function responsible to delete a worflow if one exist
     *
     * @param workflowID - worklfow id
     *
     * @throws exception in case of error
     */
    public void deleteWorkflow(String workflowId) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId);

        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        workflow.stop();
                    } finally {
                        workflow.unlock();
                    }
                    deleteWorkflow(workflow);
                    dbWrite(workflow, DbOperation.Delete);
                    String cometHost = MobiusConfig.getInstance().getCometHost();
                    String caCert = MobiusConfig.getInstance().getCometCaCert();
                    String cert = MobiusConfig.getInstance().getCometCert();
                    String certPwd = MobiusConfig.getInstance().getCometCertPwd();

                    if(cometHost != null && caCert != null && cert != null && certPwd != null) {
                        CometDataManager cometDataManager = new CometDataManager(cometHost, caCert, cert, certPwd);
                        cometDataManager.resetCometContext(workflowId);
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to return status of a worflow if one exists
     *
     * @param workflowID - worklfow id
     *
     * @throws exception in case of error
     */
    public String getWorkflowStatus(String workflowId) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId);

        String retVal = null;
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }

            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        retVal = workflow.status();
                        dbWrite(workflow, DbOperation.Update);
                    } finally {
                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT retVal=" + retVal);
            PeriodicProcessingThread.releaseLock();
        }
        return retVal;
    }

    /*
     * @brief function responsible to provision compute resources for a workflow
     *
     * @param workflowID - worklfow id
     * @param request - compute request
     *
     * @throws exception in case of error
     */
    public void processComputeRequest(String workflowId, ComputeRequest request) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        ComputeResponse response = workflow.processComputeRequest(request, false);
                        dbWrite(workflow, DbOperation.Update);
                        String cometHost = MobiusConfig.getInstance().getCometHost();
                        String caCert = MobiusConfig.getInstance().getCometCaCert();
                        String cert = MobiusConfig.getInstance().getCometCert();
                        String certPwd = MobiusConfig.getInstance().getCometCertPwd();

                       if(cometHost != null && caCert != null && cert != null && certPwd != null) {
                            CometDataManager cometDataManager = new CometDataManager(cometHost, caCert, cert, certPwd);
                            cometDataManager.createCometEntry(workflowId, request.getIpAddress(), response.getHostNames().keySet(), request.getCometFamily());
                        }
                    } finally {

                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to stitch exogeni nodes in a workflow
     *
     * @param workflowID - worklfow id
     * @param request - stitch request
     *
     * @throws exception in case of error
     */
    public void processStitchRequest(String workflowId, StitchRequest request) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        workflow.processStitchRequest(request, false);
                        dbWrite(workflow, DbOperation.Update);
                    } finally {

                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to push scripts to node in a workflow
     *
     * @param workflowID - worklfow id
     * @param request - script request
     *
     * @throws exception in case of error
     */
    public void processScriptRequest(String workflowId, ScriptRequest request) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        workflow.processScriptRequest(request, false);
                        dbWrite(workflow, DbOperation.Update);
                    } finally {

                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to provision network resources for a workflow
     *
     * @param workflowID - worklfow id
     * @param request - network request
     *
     * @throws exception in case of error
     */
    public void processNetworkRequest(String workflowId, NetworkRequest request) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    LOGGER.debug("workflow found; locking workflow");
                    workflow.lock();
                    try {
                        LOGGER.debug("invoking workflow processNetwork");
                        workflow.processNetworkRequest(request, false);
                        LOGGER.debug("workflow processNetwork completed successfully");
                        dbWrite(workflow, DbOperation.Update);
                    } finally {
                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief function responsible to provision storage resources for a workflow
     *
     * @param workflowID - worklfow id
     * @param request - storage request
     *
     * @throws exception in case of error
     */
    public void processStorageRequest(String workflowId, StorageRequest request) throws Exception{
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        workflow.processStorageRequest(request, false);
                        dbWrite(workflow, DbOperation.Update);
                    } finally {

                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    public void processSdxPrefix(String workflowId, SdxPrefix request) throws Exception {
        LOGGER.debug("IN workflowId=" + workflowId + " request=" + request);
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }
            if (workflowId != null) {
                Workflow workflow = null;
                synchronized (this) {
                    workflow = workflowHashMap.get(workflowId);
                }
                if (workflow != null) {
                    workflow.lock();
                    try {
                        workflow.processSdxPrefix(request);
                        dbWrite(workflow, DbOperation.Update);
                    } finally {

                        workflow.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }

        }
        finally {
            LOGGER.debug("OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    /*
     * @brief performs following periodic actions
     *        - Reload hostnames of all instances
     *        - Reload hostNameToSliceNameHashMap
     *        - Determine if notification to pegasus should be triggered
     *        - Build notification JSON object
     *        - launch future requests
     *
     * @return JSONObject representing notification for context to be sent to pegasus
     */
    public void doPeriodic() {
        LOGGER.debug("IN");
        synchronized (this) {
            for(HashMap.Entry<String, Workflow> workflowEntry : workflowHashMap.entrySet()) {
                Workflow workflow = workflowEntry.getValue();
                try {
                    workflow.lock();
                    workflow.doPeriodic();
                    dbWrite(workflow, DbOperation.Update);
                }
                catch (Exception e) {
                    LOGGER.debug("Exception occured while processing worklfow =" + e);
                    e.printStackTrace();
                }
                finally {
                    workflow.unlock();
                }
            }
        }
        LOGGER.debug("OUT");
    }

    /*
     * @brief reload the controller by loading all workflows from database on process restart
     */
    public void recover() {
        if(service != null) {
            List<WorkflowEntity> workflowEntities = service.getAllWorkflows();
            if (workflowEntities != null) {
                for (WorkflowEntity workflowEntity : workflowEntities) {
                    Workflow workflow = new Workflow(workflowEntity);
                    workflow.doPeriodic();
                    synchronized (this) {
                        workflowHashMap.put(workflowEntity.getWorkflowId(), workflow);
                    }
                }
            }
            LOGGER.error("recovery successful and complete");
        }
        else {
            LOGGER.error("recovery failed");
            LOGGER.error("service is null");
        }
    }

    /*
     * @brief start threads
     */
    public static void startThreads() {
        LOGGER.debug("IN");

        MobiusController.getInstance().recover();

        // create service for various periodic threads that are daemon threads
        // that way we don't have to kill them on exit
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                thread.setName("PeriodicPoolThread");
                return thread;
            }
        });

        LOGGER.debug("Staring Periodic Processing thread at " + PeriodicProcessingThread.getPeriod() + " sec.");
        ppt = new PeriodicProcessingThread();
        pptFuture = scheduler.scheduleAtFixedRate(ppt, PeriodicProcessingThread.getPeriod() , PeriodicProcessingThread.getPeriod(),
                TimeUnit.SECONDS);
        LOGGER.debug("OUT");
    }
}
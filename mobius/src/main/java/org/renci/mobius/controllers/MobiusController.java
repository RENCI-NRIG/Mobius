package org.renci.mobius.controllers;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class MobiusController {
    private HashMap<String, Workflow> workflowHashMap;
    private static final MobiusController fINSTANCE = new MobiusController();
    private static final Logger LOGGER = Logger.getLogger( MobiusController.class.getName() );

    private MobiusController() {
        workflowHashMap = new HashMap<String, Workflow>();
    }

    // thread for syncing tags from existing reservations
    protected static PeriodicProcessingThread ppt = null;
    protected static ScheduledFuture<?> pptFuture = null;


    public static MobiusController getInstance() {
        return fINSTANCE;
    }

    public void createWorkflow(String workflowID) throws Exception{
        LOGGER.debug("createWorkflow(): IN");
        if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
            LOGGER.debug("createWorkflow(): OUT");
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
            }
        }
        catch (Exception e) {
            throw new MobiusException("Internal Server Error");
        }
        finally {
            LOGGER.debug("createWorkflow(): OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    public void deleteWorkflow(Workflow workflow) {
        LOGGER.debug("deleteWorkflow(): IN");
        if(workflow != null) {
            synchronized (this) {
                workflowHashMap.remove(workflow.getWorkflowID());
            }
        }
        LOGGER.debug("deleteWorkflow(): OUT");
    }

    public void deleteWorkflow(String workflowId) throws Exception {
        LOGGER.debug("deleteWorkflow(): IN");

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
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("deleteWorkflow(): OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    public String getWorkflowStatus(String workflowId) throws Exception {
        LOGGER.debug("getWorkflowStatus(): IN");

        String retVal = null;
        try {
            if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
                throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
            }

            if (workflowId != null) {
                Workflow w = null;
                synchronized (this) {
                    w = workflowHashMap.get(workflowId);
                }
                if (w != null) {
                    w.lock();
                    try {
                        retVal = w.status();
                    } finally {
                        w.unlock();
                    }
                } else {
                    throw new MobiusException(HttpStatus.NOT_FOUND, "Workflow does not exist");
                }
            } else {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "WorkflowId is required");
            }
        }
        finally {
            LOGGER.debug("getWorkflowStatus(): OUT");
            PeriodicProcessingThread.releaseLock();
        }
        return retVal;
    }

    public void processComputeRequest(String workflowId, ComputeRequest request) throws Exception {
        LOGGER.debug("processComputeRequest(): IN");
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
                        workflow.processComputeRequest(request, false);
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
            LOGGER.debug("processComputeRequest(): OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }
    public void processNetworkRequest() throws Exception {
    }
    public void processStorageRequest(String workflowId, StorageRequest request) throws Exception{
        LOGGER.debug("processStorageRequest(): IN");
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
            LOGGER.debug("processStorageRequest(): OUT");
            PeriodicProcessingThread.releaseLock();
        }
    }

    public void doPeriodic() {
        LOGGER.debug("doPeriodic(): IN");
        synchronized (this) {
            for(HashMap.Entry<String, Workflow> workflowEntry : workflowHashMap.entrySet()) {
                Workflow w = workflowEntry.getValue();
                try {
                    w.lock();
                    w.doPeriodic();
                }
                catch (Exception e) {
                    LOGGER.debug("Exception occured while processing worklfow =" + e);
                    e.printStackTrace();
                }
                finally {
                    w.unlock();
                }
            }
        }
        LOGGER.debug("doPeriodic(): OUT");
    }

    public static void startThreads() {
        LOGGER.debug("startThreads(): IN");

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
        LOGGER.debug("startThreads(): OUT");
    }
}
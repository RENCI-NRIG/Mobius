package org.renci.mobius.controllers;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

public class MobiusController {
    private HashMap<String, Workflow> workflowHashMap;
    private static final MobiusController fINSTANCE = new MobiusController();
    private MobiusController() {
        workflowHashMap = new HashMap<String, Workflow>();
    }

    // thread for syncing tags from existing reservations
    protected static PeriodicProcessingThread ppt = null;
    protected static ScheduledFuture<?> pptFuture = null;


    public static MobiusController getInstance() {
        return fINSTANCE;
    }

    public String createWorkflowID() throws Exception{
        if (!PeriodicProcessingThread.tryLock(PeriodicProcessingThread.getWaitTime())) {
            throw new MobiusException(HttpStatus.SERVICE_UNAVAILABLE, "system is busy, please try again in a few minutes");
        }
        String uuid = null;
        try {
            synchronized (this) {
                uuid = java.util.UUID.randomUUID().toString();
                // TODO
                uuid = "acdbaada-7a4a-47dd-9832-32c04153c766";
                Workflow workflow = new Workflow(uuid);
                workflowHashMap.put(uuid, workflow);
            }
        }
        catch (Exception e) {
            throw new MobiusException("Internal Server Error");
        }
        finally {
            PeriodicProcessingThread.releaseLock();
        }
        return uuid;
    }

    public void deleteWorkflow(Workflow workflow) {
        if(workflow != null) {
            synchronized (this) {
                workflowHashMap.remove(workflow.getWorkflowID());
            }
        }
    }

    public void deleteWorkflow(String workflowId) throws Exception {
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
            PeriodicProcessingThread.releaseLock();
        }
    }

    public String getWorkflowStatus(String workflowId) throws Exception {
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
            PeriodicProcessingThread.releaseLock();
        }
        return retVal;
    }

    public void processComputeRequest(String workflowId, ComputeRequest request) throws Exception {
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
                        workflow.processComputeRequest(request);
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
            PeriodicProcessingThread.releaseLock();
        }
    }
    public void processNetworkRequest() throws Exception {
    }
    public void processStorageRequest(String workflowId, StorageRequest request) throws Exception{
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
                        workflow.processStorageRequest(request);
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
            PeriodicProcessingThread.releaseLock();
        }
    }

    public void doPeriodic() {
        synchronized (this) {
            for(HashMap.Entry<String, Workflow> workflowEntry : workflowHashMap.entrySet()) {
                Workflow w = workflowEntry.getValue();
                try {
                    w.lock();
                    w.doPeriodic();
                }
                catch (Exception e) {
                    System.out.println("Exception occured while processing worklfow =" + e);
                }
                finally {
                    w.unlock();
                }
            }
        }
    }

    public static void startThreads() {

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

        System.out.println("Staring Periodic Processing thread at " + PeriodicProcessingThread.getPeriod() + " sec.");
        ppt = new PeriodicProcessingThread();
        pptFuture = scheduler.scheduleAtFixedRate(ppt, PeriodicProcessingThread.getPeriod() , PeriodicProcessingThread.getPeriod(),
                TimeUnit.SECONDS);

    }
}
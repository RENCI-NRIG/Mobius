package org.renci.mobius.controllers;

import java.util.concurrent.Semaphore;

/**
 * Objects of this class are used to ensure that slice operations are executed in the order that they arrived (at the
 * REST interface)
 *
 * @author kthare10
 *
 */
public class WorkflowOperationLock extends Semaphore {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public WorkflowOperationLock() {
        // create a fair semaphore
        super(1, true);
    }
}
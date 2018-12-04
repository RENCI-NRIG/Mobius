/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.policy;

import org.apache.log4j.Logger;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.RMState;
import org.renci.requestmanager.ndl.AhabManager;

/**
 *
 * @author anirban
 */
public class SimpleUnitsModifyPolicy implements IModifyPolicy{

    Logger logger = null;
    
    public SimpleUnitsModifyPolicy(Logger logger){
        this.logger = logger;
    }
    
    /*
    * Determines the change innumber of workers, given the current manifest and the current requirements from application;
    * It finds the total number of currently active workers and the total number of currently ticketed workers to gauge the current 
    * total number of potentially active workers
    * This also takes into account the number of nodes that would be deleted soon
    */
    @Override
    public int determineChangeInNumWorkers(ModifyRequestInfo modReq, String manifest) {
        
        AhabManager ahabManager = new AhabManager();
        int numActiveWorkersInManifest = ahabManager.getNumActiveWorkersInManifest(manifest);        
        int numTicketedWorkersInManifest = ahabManager.getNumTicketedWorkersInManifest(manifest);
        int numActivePlusTicketedWorkers = numActiveWorkersInManifest + numTicketedWorkersInManifest;
        
        int userViewNumActiveWorkers = modReq.getNumCurrentRes();
        int userViewNumWorkersReqdToMeetDeadline = modReq.getNumResReqToMeetDeadline();
        
        if(numActiveWorkersInManifest < 0 || userViewNumActiveWorkers < 0 || userViewNumWorkersReqdToMeetDeadline < 0){
            logger.error("SimpleUnitsModifyPolicy encountered negative numActiveWorkersInManifest, userViewNumActiveWorkers, or userViewNumWorkersReqdToMeetDeadline...");
            logger.error("SimpleUnitsModifyPolicy returning change = 0...");
            return 0;
        }
        
        RMState rmState = RMState.getInstance();
        int numActiveToBeDeletedWorkers = rmState.getNumNodesToBeDeleted().intValue();
        
        logger.info("numActiveWorkersInManifest = " + numActiveWorkersInManifest + " | numTicketedWorkersInManifest = " + numTicketedWorkersInManifest);
        logger.info("numActiveToBeDeletedWorkers = " + numActiveToBeDeletedWorkers);
        logger.info("numActivePlusTicketedWorkers = " + numActivePlusTicketedWorkers + " | userViewNumWorkersReqdToMeetDeadline = " + userViewNumWorkersReqdToMeetDeadline + " | userViewNumActiveWorkers = " + userViewNumActiveWorkers);
        
        return (userViewNumWorkersReqdToMeetDeadline - numActivePlusTicketedWorkers + numActiveToBeDeletedWorkers);
        
    }
    
}

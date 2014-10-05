/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.policy;

import org.apache.log4j.Logger;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.ndl.NdlLibManager;

/**
 *
 * @author anirban
 */
public class SimpleUnitsModifyPolicy implements IModifyPolicy{

    Logger logger = null;
    
    public SimpleUnitsModifyPolicy(Logger logger){
        this.logger = logger;
    }
    
    @Override
    public int determineChangeInNumWorkers(ModifyRequestInfo modReq, String manifest) {
        
        NdlLibManager ndlManager = new NdlLibManager();
        int numActiveWorkersInManifest = ndlManager.getNumActiveWorkersInManifest(manifest);
        
        int userViewNumActiveWorkers = modReq.getNumCurrentRes();
        int userViewNumWorkersReqdToMeetDeadline = modReq.getNumResReqToMeetDeadline();
        
        if(numActiveWorkersInManifest < 0 || userViewNumActiveWorkers < 0 || userViewNumWorkersReqdToMeetDeadline < 0){
            logger.error("SimpleUnitsModifyPolicy encountered negative numActiveWorkersInManifest, userViewNumActiveWorkers, or userViewNumWorkersReqdToMeetDeadline...");
            logger.error("SimpleUnitsModifyPolicy returning change = 0...");
            return 0;
        }
        
        logger.info("numActiveWorkersInManifest = " + numActiveWorkersInManifest + " | userViewNumWorkersReqdToMeetDeadline = " + userViewNumWorkersReqdToMeetDeadline + " | userViewNumActiveWorkers = " + userViewNumActiveWorkers);
        
        return (userViewNumWorkersReqdToMeetDeadline - numActiveWorkersInManifest);
        
    }
    
}

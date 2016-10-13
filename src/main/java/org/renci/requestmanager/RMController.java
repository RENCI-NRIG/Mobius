/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.renci.requestmanager.amqp.DisplayPublisher;
import org.renci.requestmanager.amqp.ManifestPublisherOnDemand;
import org.renci.requestmanager.ndl.AhabManager;
import org.renci.requestmanager.ndl.NdlLibManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaSMXMLRPCProxy;

/**
 *
 * @author anirban
 */
public class RMController implements RMConstants{

        private static ArrayList<Timer> timers = new ArrayList<Timer>();
	private static boolean noStart = false;

        protected RMState rmState = null;

        protected Logger logger = null;

        private static Properties rmProperties = null;


        public RMController(Properties rmProps){

		initialize();

                logger = Logger.getLogger(this.getClass());
                logger.info("RMController created..");

                this.rmProperties = rmProps;

		Timer timer = null;
		synchronized(timers) {
			if (noStart)
				return;
			timer = new Timer("RMControllerTask", true);
			timers.add(timer);
		}
		//timer.schedule(new RMControllerTask(), 30*1000, 30*1000); // run every 30 seconds after 30 seconds
                timer.schedule(new RMControllerTask(), 0, 30*1000); // run every 30 seconds
	}

	private void allStop() {
		logger.info("Shutting down RMController timer threads");
		synchronized(timers) {
			noStart=true;
			for (Timer t: timers) {
				t.cancel();
			}
			timers.clear();
		}
	}

	private void initialize() {

	}


	class RMControllerTask extends TimerTask {

		public void run() {

                    try{

                        logger.info("RMController timer thread ..... : START");
                        System.out.println("RMController timer thread ..... : START");

                        rmState = RMState.getInstance();
                        
                        ArrayList<AppRequestInfo> currReqQ = rmState.getAppReqQ();
                        
                        synchronized(currReqQ){

                            if(currReqQ == null){
                                return; // nothing to do if there are no requests to watch
                            }
                            if(currReqQ.size() <= 0){ // nothing in the queue
                                return;
                            }

                            for(AppRequestInfo currAppRequestInfo: currReqQ){ // Go through all the app requests

                                if(!currAppRequestInfo.isProcessed()){ // If request has not been processed already

                                    String currOrcaSliceId = currAppRequestInfo.getOrcaSliceID();
                                    System.out.println("Current slice = " + currOrcaSliceId);
                                    logger.info("Current slice = " + currOrcaSliceId);
                                    
                                    if(currAppRequestInfo.getNewReq() != null && currAppRequestInfo.getModifyReq() != null){
                                        logger.error("App request can't have both modify and new slice request at the same time");
                                        continue;
                                    }
                                    
                                    if(currAppRequestInfo.getNewReq() != null){ // this is a first time request to create a new slice
                                        
                                        NewRequestInfo newReq = currAppRequestInfo.getNewReq();
                                        String resultStatus = processNewReq(newReq, currOrcaSliceId);
                                        // If the create action needs to be delayed, don't set setProcessed to true
                                        // Only if it is either a success or an error condition, set setProcessed to true
                                        if(resultStatus.equalsIgnoreCase("SUCCESS") || resultStatus.equalsIgnoreCase("ERROR")){
                                            currAppRequestInfo.setProcessed(true);
                                        }
                                        continue;
                                                                                
                                    }
                                    
                                    if(currAppRequestInfo.getModifyReq() != null){ // this is a modify request for an existing slice
                                        
                                        ModifyRequestInfo modReq = currAppRequestInfo.getModifyReq();
                                        String modifyType = modReq.getModifyType();
                                        if(modifyType.equalsIgnoreCase("modifyCompute")){
                                            String resultStatus = processModifyComputeReq(modReq, currOrcaSliceId);
                                            // If the modify action needs to be delayed, don't set setProcessed to true
                                            // Only if it is either a success or an error condition, set setProcessed to true
                                            if(resultStatus.equalsIgnoreCase("SUCCESS") || resultStatus.equalsIgnoreCase("ERROR")){
                                                currAppRequestInfo.setProcessed(true);
                                            }
                                        }
                                        
                                        if(modifyType.equalsIgnoreCase("modifyNetwork")){
                                            String resultStatus = processModifyNetworkReq(modReq, currOrcaSliceId);
                                            // If the modify action needs to be delayed, don't set setProcessed to true
                                            // Only if it is either a success or an error condition, set setProcessed to true
                                            if(resultStatus.equalsIgnoreCase("SUCCESS") || resultStatus.equalsIgnoreCase("ERROR")){
                                                currAppRequestInfo.setProcessed(true);
                                            }
                                        }
                                        
                                        continue;
                                                                                
                                    }
                                    
                                    if(currAppRequestInfo.getLinkReq() != null){ // this is a request to add a link
                                        
                                        LinkRequestInfo linkReq = currAppRequestInfo.getLinkReq();
                                        processLinkReq(linkReq);
                                        currAppRequestInfo.setProcessed(true);
                                        continue;
                                                                                
                                    }
                                    
                                    // In future there can be many other kinds of modify requests, which would be handled here
                                    
                                    if(currAppRequestInfo.getDeleteReq() != null){ // this is a delete request for an existing slice
                                        
                                        DeleteRequestInfo deleteReq = currAppRequestInfo.getDeleteReq();
                                        String resultStatus = processDeleteReq(deleteReq, currOrcaSliceId);
                                        // trying delete only once; so no need to conditionally set setProcessed
                                        currAppRequestInfo.setProcessed(true);
                                        continue;
                                                                                
                                    }
                                    
                            
                                }

                            } // end for all requests in the queue 
                            
                        } // end synchronized                     

                    } catch (Exception e){
                        logger.error("Exception occured during Timer thread execution of RMController: Stack trace follows... " , e);
                        logger.error("Continuing execution beyond Exception");
			System.out.println("There was an exception during Timer thread execution of RMController");
                    }



                }

                
                /*
                * Processes a new user request, generates appropriate ndl request and sends it to ORCA/ExoGENI
                * Uses ndlLib; Need to use ahab in future
                */
                
                private String processNewReq(NewRequestInfo newReq, String orcaSliceID) {
                    
                    if(newReq == null || orcaSliceID == null || orcaSliceID.isEmpty()){
                        logger.error("newReq or orcaSliceID is null.. new user request not processed..");
                        return "ERROR";
                    }
                    
                    String requestedTemplateType = newReq.getTemplateType();
                    
                    if (requestedTemplateType == null){
                        logger.error("requestedTemplateType is null.. new user request not processed..");
                        return "ERROR";
                    }
                    
                    if(requestedTemplateType.startsWith(SDXCondorBasicTypeName)){
                        
                        AhabManager ahabManager = new AhabManager(rmProperties);
                        logger.info("Calling ahab to create slice for a request with basic type = " + SDXCondorBasicTypeName);
                        String resultStatus = ahabManager.processNewSDXCondor(newReq, orcaSliceID);
                        if (resultStatus != null){
                            return "SUCCESS";
                        }
                        else{
                            return "ERROR";
                        }
                    }
                    else {
                        // Depending on the template type, call ndllib to generate appropriate request
                        NdlLibManager ndlManager = new NdlLibManager(rmProperties);
                        String ndlReq = null;

                        if(requestedTemplateType.startsWith(CondorBasicTypeName)){
                            logger.info("Calling ndllib to generate a request with basic type = " + CondorBasicTypeName);
                            ndlReq = ndlManager.generateNewCondorRequest(newReq);
                        }
                        else if(requestedTemplateType.startsWith(HadoopBasicTypeName)){
                            logger.info("Calling ndllib to generate a request with basic type = " + HadoopBasicTypeName);
                            ndlReq = ndlManager.generateNewHadoopRequest(newReq);
                        }
                        else if(requestedTemplateType.startsWith(MPIBasicTypeName)){
                            logger.info("Calling ndllib to generate a request with basic type = " + MPIBasicTypeName);
                            ndlReq = ndlManager.generateNewMPIRequest(newReq);
                        }
                        else {
                            logger.error("Unsupported requested template type");
                            return "ERROR";
                        }

                        // Send request to ExoGENI
                        logger.info("Sending request to ExoGENI...");

                        DisplayPublisher dp;
                        try {
                            dp = new DisplayPublisher(rmProperties);
                            dp.publishInfraMessages(orcaSliceID, "Sending request for new slice: " + orcaSliceID + " to ExoGENI...");
                        } catch (Exception ex) {
                            logger.error("Exception while publishing to display exchange");
                        }

                        OrcaManager orcaManager = new OrcaManager(rmProperties);
                        String resultCreate = orcaManager.sendCreateRequestToORCA(orcaSliceID, ndlReq);

                        //Push orcaSliceID to manifest publishing queue
                        if(resultCreate != null){

                            // Publish manifest on demand
                            try {
                                ManifestPublisherOnDemand mPublisherOnDemand = new ManifestPublisherOnDemand(rmProperties);
                                mPublisherOnDemand.getManifestAndPublishManifestData(orcaSliceID);
                            } catch (Exception ex) {
                                logger.error("Exception while publishing manifest on demand " + ex);
                            }    

                            rmState = RMState.getInstance();
                            rmState.addSliceIDToSliceIDQ(orcaSliceID);
                            logger.info("Added " + orcaSliceID + " to SliceIDQ...");
                            logger.info("Done processing new user request");
                            return "SUCCESS";
                        }
                        else{ // there was an exception
                            logger.info("Can't create slice at this moment.. trying again in 30 seconds..");
                            return "DELAY";
                        }
                    }                    
                    
                }
                
                // This uses ndlLib; Need to use AHAB in future
                
                private String processModifyComputeReq(ModifyRequestInfo modReq, String orcaSliceID) {
                    
                    if(modReq == null || orcaSliceID == null || orcaSliceID.isEmpty()){
                        logger.error("modReq or orcaSliceID is null.. modify request not processed..");
                        return "ERROR";
                    }
                    
                    // Getting current manifest from ExoGENI
                    logger.info("Getting manifest from ExoGENI for slice: " + orcaSliceID);
                    OrcaManager orcaManager = new OrcaManager(rmProperties);
                    String currManifest = orcaManager.getManifestFromORCA(orcaSliceID);
                    
                    if(currManifest == null){
                        logger.error("manifest for slice: " + orcaSliceID + " is null.. modify request not processed..");
                        return "ERROR";
                    }
                    
                    // Call ndllib to generate modify request
                    NdlLibManager ndlManager = new NdlLibManager(rmProperties);
                    String ndlModReq = null;
                    
                    // Commenting the next block because we want to allow modify even when the slice is not ready
                    /*
                    logger.info("Calling ndllib to get sliceStatus for " + orcaSliceID);
                    String sliceStatus = ndlManager.getSliceManifestStatus(currManifest);
                    if(!sliceStatus.equalsIgnoreCase("ready")){ // slice is not ready, don't allow modify actions now
                        logger.info("Slice manifest says that slice is not ready.. Delaying modify actions..");
                        return "DELAY";
                    }
                    */
                    
                    logger.info("Calling ndllib to generate a modify request for slice: " + orcaSliceID);
                    ndlModReq = ndlManager.generateModifyComputeRequest(modReq, currManifest);
                    
                    DisplayPublisher dp;
                    if(ndlModReq == null){
                        logger.info("No ORCA modify request was generated by ndllib and modify policies.. Not sending any modify request to ExoGENI..");
                        
                        try {
                            dp = new DisplayPublisher(rmProperties);
                            dp.publishInfraMessages(orcaSliceID, "No ORCA modify request was generated by ndllib and modify policies for " + orcaSliceID + ".... Not sending any modify request to ExoGENI...");
                        } catch (Exception ex) {
                            logger.error("Exception while publishing to display exchange");
                        }
                        
                        return "ERROR";
                    }
                    
                    // We have a modify request; Send modify request to ExoGENI
                    logger.info("Sending modifyComute request to ExoGENI...");
                    
                    
                    try {
                        dp = new DisplayPublisher(rmProperties);
                        dp.publishInfraMessages(orcaSliceID, "Sending modifyCompute request for slice: " + orcaSliceID + " to ExoGENI...");
                    } catch (Exception ex) {
                        logger.error("Exception while publishing to display exchange");
                    }
                    
                    String resultModify = orcaManager.sendModifyRequestToORCA(orcaSliceID, ndlModReq);                    
                    
                    if(resultModify != null){
                        
                        // Publish manifest on demand
                        try {
                            ManifestPublisherOnDemand mPublisherOnDemand = new ManifestPublisherOnDemand(rmProperties);
                            mPublisherOnDemand.getManifestAndPublishManifestData(orcaSliceID);
                        } catch (Exception ex) {
                            logger.error("Exception while publishing manifest on demand " + ex);
                        }                       
                        
                        // To make sure that manifest data for a slice created by an entity external to RM is also published
                        // when a modify request for that slice comes to RM
                        rmState = RMState.getInstance();
                        if(!rmState.isInSliceIDQ(orcaSliceID)){ // if not in manifest publishing queue already
                            rmState.addSliceIDToSliceIDQ(orcaSliceID);
                            logger.info("Added " + orcaSliceID + " to SliceIDQ...");
                        }
                        logger.info("Done processing modifyCompute request");
                        return "SUCCESS";
                    }
                    else{ // there was an exception
                        logger.info("Can't modify slice at this moment... trying again in 30 seconds..");
                        return "DELAY";
                    }
                    
                }

                // This uses AHAB
                private String processModifyNetworkReq(ModifyRequestInfo modReq, String orcaSliceID) {
                    
                    if(modReq == null || orcaSliceID == null || orcaSliceID.isEmpty()){
                        logger.error("modReq or orcaSliceID is null.. modify request not processed..");
                        return "ERROR";
                    }
                    
                    // Call ahab manager function to actuate network modification
                    AhabManager ahabManager = new AhabManager(rmProperties);
                    
                    String ndlModReq = null;
                    
                    return null;
                }
                
                private void processLinkReq(LinkRequestInfo linkReq) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                private String processDeleteReq(DeleteRequestInfo deleteReq, String orcaSliceID){
                    
                    if(deleteReq == null || orcaSliceID == null || orcaSliceID.isEmpty()){
                        logger.error("deleteReq or orcaSliceID is null.. delete request not processed..");
                        return "ERROR";
                    }
                    // Send delete request to ExoGENI
                    logger.info("Sending slice delete request to ExoGENI...");
                    OrcaManager orcaManager = new OrcaManager(rmProperties);
                    boolean deleteSuccessful = orcaManager.sendDeleteRequestToORCA(orcaSliceID);
                    if(deleteSuccessful){
                        rmState = RMState.getInstance();
                        if(rmState.isInSliceIDQ(orcaSliceID)){ // if not in manifest publishing queue already
                            rmState.deleteSliceIDFromSliceIDQ(orcaSliceID);
                            logger.info("Removed " + orcaSliceID + " from SliceIDQ...");
                        }
                        return "SUCCESS";
                    }
                    
                    return "ERROR";
                }
                
                
        }    
    
    
}

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
import org.apache.log4j.Logger;
import org.renci.requestmanager.ndl.NdlLibManager;
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
		timer.schedule(new RMControllerTask(), 30*1000, 30*1000); // run every 30 seconds
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
                                        processNewReq(newReq, currOrcaSliceId);
                                        currAppRequestInfo.setProcessed(true);
                                        continue;
                                                                                
                                    }
                                    
                                    if(currAppRequestInfo.getModifyReq() != null){ // this is a modify request for an existing slice
                                        
                                        ModifyRequestInfo modReq = currAppRequestInfo.getModifyReq();
                                        processModReq(modReq);
                                        currAppRequestInfo.setProcessed(true);
                                        continue;
                                                                                
                                    }
                                    
                                    if(currAppRequestInfo.getLinkReq() != null){ // this is a request to add a link
                                        
                                        LinkRequestInfo linkReq = currAppRequestInfo.getLinkReq();
                                        processLinkReq(linkReq);
                                        currAppRequestInfo.setProcessed(true);
                                        continue;
                                                                                
                                    }
                                    
                                    // In future there can be many other kinds of modify requests, which would be handled here
                            
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
                */
                
                private void processNewReq(NewRequestInfo newReq, String orcaSliceID) {
                    
                    if(newReq == null || orcaSliceID == null || orcaSliceID.isEmpty()){
                        logger.error("newReq or orcaSliceID is null.. new user request not processed..");
                        return;
                    }
                    
                    String requestedTemplateType = newReq.getTemplateType();
                    
                    if (requestedTemplateType == null){
                        logger.error("requestedTemplateType is null.. new user request not processed..");
                        return;
                    }
                    
                    // Depending on the template type, call ndllib to generate appropriate request
                    NdlLibManager ndlManager = new NdlLibManager();
                    String ndlReq = null;
                    
                    if(requestedTemplateType.startsWith(CondorBasicTypeName)){
                        logger.info("Calling ndllib to generate a request with basic type = " + CondorBasicTypeName);
                        ndlReq = ndlManager.generateNewCondorRequest(newReq, logger);
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
                        return;
                    }
                    
                    // Send request to ExoGENI
                    logger.info("Sending request to ExoGENI...");
                    sendCreateRequestToORCA(orcaSliceID, ndlReq);                    
                    logger.info("Done processing new user request");
                    
                }

                private void processModReq(ModifyRequestInfo modReq) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                private void processLinkReq(LinkRequestInfo linkReq) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                /*
                * Sends ndl request to a specific controller
                */
                private void sendCreateRequestToORCA(String sliceId, String controllerUrl, String createReq){

                    if(controllerUrl == null || controllerUrl.isEmpty()){
                        controllerUrl = RMConstants.defaultControllerUrl;
                    }
                    
                    String createRes = null;
                    try {
                        OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
                        orcaProxy.setControllerUrl(controllerUrl);
                        createRes = orcaProxy.createSlice(sliceId, createReq);
                        logger.info("Result for create slice for " + sliceId + " = " + createRes);
                        System.out.println("Result for modify slice for " + sliceId + " = " + createRes);
                    } catch (Exception ex) {
                        logger.error("Exception while calling ORCA createSlice" + ex);
                        System.out.println("Exception while calling ORCA createSlice" + ex);
                        return;
                    }

                }
                
                /*
                * Sends ndl request to the default controller
                */
                private void sendCreateRequestToORCA(String sliceId, String createReq){
                    
                    String controllerUrl = RMConstants.defaultControllerUrl;
                                       
                    String createRes = null;
                    try {
                        OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
                        orcaProxy.setControllerUrl(controllerUrl);
                        createRes = orcaProxy.createSlice(sliceId, createReq);
                        logger.info("Result for create slice for " + sliceId + " = " + createRes);
                        System.out.println("Result for modify slice for " + sliceId + " = " + createRes);
                    } catch (Exception ex) {
                        logger.error("Exception while calling ORCA createSlice" + ex);
                        System.out.println("Exception while calling ORCA createSlice" + ex);
                        return;
                    }

                }
                
                
        }    
    
    
}

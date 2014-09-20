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
                                        processNewReq(newReq);
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

                
                private void processNewReq(NewRequestInfo newReq) {
                    
                    
                    
                    
                }

                private void processModReq(ModifyRequestInfo modReq) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                private void processLinkReq(LinkRequestInfo linkReq) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }


                
        }    
    
    
}

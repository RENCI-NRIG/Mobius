/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author anirban
 */
public class RMState implements Serializable {

        private ArrayList<AppRequestInfo> appReqQ = new ArrayList<AppRequestInfo>(); // keeps track of user/application requests (new, modify)
        private ArrayList<String> sliceIDQ = new ArrayList<String>();

        // use output compression
        private static boolean compressOutput = true;

        private static final RMState fINSTANCE =  new RMState();

        private RMState(){
                // Can't call this constructor
        }

        public static RMState getInstance() {
            return fINSTANCE;
        }

        public ArrayList<AppRequestInfo> getAppReqQ() {
            return appReqQ;
        }

        public void setAppReqQ(ArrayList<AppRequestInfo> appReqQ) {
            this.appReqQ = appReqQ;
        }
        
        public void addReqToAppReqQ(AppRequestInfo newReq){
            synchronized(appReqQ){
                appReqQ.add(newReq);
            }
        }

        public boolean deleteReqFromAppReqQ(AppRequestInfo reqInfo){
            synchronized(appReqQ){
                return(appReqQ.remove(reqInfo));
            }
        }
        
        /*
        * Returns an AppRequestInfo corresponding to an unprocessed modify request with same (sliceID, wfuuid) combo
        */
        public AppRequestInfo findAppReqForModifyFromAppReqQ(String sliceID, String wfuuid){
            
            synchronized(appReqQ){
                for(AppRequestInfo currAppRequestInfo: appReqQ){
                    if((currAppRequestInfo.getModifyReq() != null) && (!currAppRequestInfo.isProcessed())){ // This AppRequestInfo corresponds to an unprocessed modify request
                        String existingSliceID = currAppRequestInfo.getModifyReq().getOrcaSliceId();
                        String existingWfuuid = currAppRequestInfo.getModifyReq().getWfUuid();
                        if(existingSliceID.equalsIgnoreCase(sliceID) && existingWfuuid.equalsIgnoreCase(wfuuid)){
                            return currAppRequestInfo;
                        }
                    }
                }
            }
            
            return null;
            
        }
 
        /*
        * Returns an AppRequestInfo corresponding to an unprocessed new request with same (sliceID, wfuuid) combo
        */
        public AppRequestInfo findAppReqForNewFromAppReqQ(String sliceID, String wfuuid){
            
            synchronized(appReqQ){
                for(AppRequestInfo currAppRequestInfo: appReqQ){
                    if((currAppRequestInfo.getNewReq() != null) && (!currAppRequestInfo.isProcessed())){ // This AppRequestInfo corresponds to an unprocessed new request
                        String existingSliceID = currAppRequestInfo.getOrcaSliceID();
                        String existingWfuuid = currAppRequestInfo.getNewReq().getWfUuid();
                        if(existingSliceID.equalsIgnoreCase(sliceID) && existingWfuuid.equalsIgnoreCase(wfuuid)){
                            return currAppRequestInfo;
                        }
                    }
                }
            }
            
            return null;
            
        }        
        
        public ArrayList<String> getSliceIDQ() {
            return sliceIDQ;
        }

        public void setSliceIDQ(ArrayList<String> sliceIDQ) {
            this.sliceIDQ = sliceIDQ;
        }
        
         public void addSliceIDToSliceIDQ(String newSliceID){
            synchronized(sliceIDQ){
                sliceIDQ.add(newSliceID);
            }
        }

        public boolean deleteSliceIDFromSliceIDQ(String sliceID){
            synchronized(sliceIDQ){
                return(sliceIDQ.remove(sliceID));
            }
        }
        
        public boolean isInSliceIDQ(String sliceID){
            synchronized(sliceIDQ){
                return(sliceIDQ.contains(sliceID));
            }
        }
        
        // manage state of compression of output
        public boolean getCompression() {
        	return compressOutput;
        }

    	public synchronized void setCompression(boolean f) {
    		compressOutput = f;
    	}

        /**
        * If the singleton implements Serializable, then this
        * method must be supplied.
        */
        private Object readResolve() throws ObjectStreamException {
            return fINSTANCE;
        }


}

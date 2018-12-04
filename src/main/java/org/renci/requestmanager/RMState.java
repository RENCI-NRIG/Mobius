/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.rabbitmq.client.ConnectionFactory;
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
        private ArrayList<String> nodesToBeDeletedQ = new ArrayList<String>();
        private Integer numNodesToBeDeleted = new Integer(0);
        private ConnectionFactory factory = new ConnectionFactory();
        
        private Table<String, String, String> crossSitePriority = HashBasedTable.create(); // a table for flow priority
        
        private ArrayList<StitchportInfo> stitchportList = new ArrayList<StitchportInfo>();

        // use output compression
        private static boolean compressOutput = true;

        private static final RMState fINSTANCE =  new RMState();

        private RMState(){
                // Can't call this constructor
        }

        public static RMState getInstance() {
            return fINSTANCE;
        }

        public ConnectionFactory getFactory() {
            return factory;
        }

        public void setFactory(ConnectionFactory factory) {
            this.factory = factory;
        }
        
        public ArrayList<AppRequestInfo> getAppReqQ() {
            return appReqQ;
        }

        public void setAppReqQ(ArrayList<AppRequestInfo> appReqQ) {
            this.appReqQ = appReqQ;
        }

        public Table<String, String, String> getCrossSitePriority() {
            return crossSitePriority;
        }

        public void setCrossSitePriority(Table<String, String, String> crossSitePriority) {
            this.crossSitePriority = crossSitePriority;
        }       

        public ArrayList<StitchportInfo> getStitchportList() {
            return stitchportList;
        }

        public void setStitchportList(ArrayList<StitchportInfo> stitchportList) {
            this.stitchportList = stitchportList;
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
        
        public void addSPToStitchportList(StitchportInfo spInfo){
            synchronized(stitchportList){
                stitchportList.add(spInfo);
            }
        }
        
        public StitchportInfo findSPFromStitchportList(String orcaSliceID){
        
            synchronized(stitchportList){
                for(StitchportInfo currSPInfo: stitchportList){
                    if(currSPInfo.getOrcaSliceID().equalsIgnoreCase(orcaSliceID)){
                        return currSPInfo;
                    }
                }
            }
            
            return null;
        }
        
        public void addPriorityToCrossSitePriority(String endPointSrc, String endPointDst, String flowPriority){
            synchronized(crossSitePriority){
                crossSitePriority.put(endPointSrc, endPointDst, flowPriority);
            }
        }
        
        public String findPriorityForEndpointPairFromCrossSitePriority(String endPointSrc, String endPointDst){
            
            synchronized(crossSitePriority){
                if(crossSitePriority.contains(endPointSrc, endPointDst)){
                    return crossSitePriority.get(endPointSrc, endPointDst);
                }
            }
            
            return null;
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

        public ArrayList<String> getNodesToBeDeletedQ() {
            return nodesToBeDeletedQ;
        }

        public void setNodesToBeDeletedQ(ArrayList<String> nodesToBeDeletedQ) {
            this.nodesToBeDeletedQ = nodesToBeDeletedQ;
        }
        
         public void addNodeToNodesToBeDeletedQ(String newNodeURI){
            synchronized(nodesToBeDeletedQ){
                nodesToBeDeletedQ.add(newNodeURI);
            }
        }

        public boolean deleteNodeFromNodesToBeDeletedQ(String nodeURI){
            synchronized(nodesToBeDeletedQ){
                return(nodesToBeDeletedQ.remove(nodeURI));
            }
        }
        
        public boolean isInNodesToBeDeletedIDQ(String nodeURI){
            synchronized(nodesToBeDeletedQ){
                return(nodesToBeDeletedQ.contains(nodeURI));
            }
        }
        
        public Integer getNumNodesToBeDeleted() {
            return numNodesToBeDeleted;
        }
        
        public void increaseNumNodesToBeDeleted(){
            synchronized(numNodesToBeDeleted){
                int newVal = (numNodesToBeDeleted.intValue() + 1);
                numNodesToBeDeleted = new Integer(newVal);
            }
        }
        
        public void decreaseNumNodesToBeDeleted(){
            synchronized(numNodesToBeDeleted){
                int newVal = (numNodesToBeDeleted.intValue() - 1);
                numNodesToBeDeleted = new Integer(newVal);
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

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

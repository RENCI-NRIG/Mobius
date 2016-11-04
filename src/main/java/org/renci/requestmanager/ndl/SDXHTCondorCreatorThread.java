/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import com.rabbitmq.client.ConnectionFactory;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class SDXHTCondorCreatorThread implements Runnable, RMConstants{
    
    protected Logger logger = null;
    
    private static Properties rmProperties = null;
    private NewRequestInfo newReq = null;
    private String orcaSliceID = null;

    public SDXHTCondorCreatorThread(Properties rmProps, NewRequestInfo newRequest, String orcaSliceID) throws Exception{
        
        logger = Logger.getLogger(this.getClass());
        
        this.rmProperties = rmProps;
        this.newReq = newRequest;
        this.orcaSliceID = orcaSliceID;
        
    } 

    @Override
    public void run() {
        
        AhabManager ahabManager = new AhabManager(rmProperties);
        logger.info("SDXHTCondorCreatorThread: Calling ahab to create slice for a request with basic type = " + SDXCondorBasicTypeName);
        String resultStatus = ahabManager.processNewSDXCondor(newReq, orcaSliceID);
        if (resultStatus != null){
            logger.info("SDXHTCondorCreatorThread: Done sending all requests for SDX Condor slice");
        }
        else{
            logger.error("SDXHTCondorCreatorThread: Error while creating new SDX Condor slice..");
        }
        
    }
    
}

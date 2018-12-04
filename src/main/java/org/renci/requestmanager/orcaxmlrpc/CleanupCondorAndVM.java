/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.orcaxmlrpc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.requestmanager.RMConstants;
import org.renci.requestmanager.RMState;
import org.renci.requestmanager.amqp.DisplayPublisher;


/**
 *
 * @author anirban
 */
public class CleanupCondorAndVM implements Runnable, RMConstants{

    protected Logger logger = null;
    
    private static Properties rmProperties = null;
    
    private String orcaSliceID = null;
    private String mnIP = null;
    private String mnURI = null;
    

    public CleanupCondorAndVM(Properties rmProps, String orcaSliceID, String mnIP, String mnURI) throws Exception{
        
        logger = Logger.getLogger(this.getClass());
        
        this.rmProperties = rmProps;
        this.orcaSliceID = orcaSliceID;
        this.mnIP = mnIP;
        //When migrating to AHAB, we use the URL instead of URI, since compute nodes no longer store a URI
        this.mnURI = mnURI;        
        
    }
    
    
    @Override
    public void run() {
        
        logger.info("CLEANUP: Start ...." + orcaSliceID + "|" + mnIP + "|" + mnURI);
        
        // Increase count of numNodesToBeDeleted in RMState        
        RMState rmState = RMState.getInstance();
        rmState.increaseNumNodesToBeDeleted();
        rmState.addNodeToNodesToBeDeletedQ(mnURI);
        logger.info("CLEANUP: increased numNodestoBeDeleted in RMState");
        logger.info("CLEANUP: current total numNodestoBeDeleted = " + rmState.getNumNodesToBeDeleted());
        
        // Do ssh and run kill condor script
        logger.info("CLEANUP: Waiting on condor going down on " + mnIP);
        doSSHAndKillCondor(mnIP);
        
        // Get manifest, generate modify request, send modify request
        OrcaManager orcaManager = new OrcaManager(rmProperties);
        logger.info("CLEANUP: Getting manifest from ORCA...");
        
        String currManifest = orcaManager.getManifestFromORCA(orcaSliceID);
        if (currManifest != null){
            
            Slice s = Slice.loadManifest(currManifest);
            
            // delete a specific node from the manifest
            boolean nodeDeleted = false;
            for(ComputeNode cn: s.getComputeNodes()){
                if(cn.getURL().equalsIgnoreCase(mnURI)){
                    logger.info("CLEANUP: computeNode: " + cn.getName() + ", state = " + cn.getState());
                    logger.info("CLEANUP: deleting manifest node: " + cn.getURL());
                    cn.delete();
                    nodeDeleted = true;
                }                
            }
            
            // If node got deleted, send modify request to ORCA
            if(nodeDeleted){
                String modReq = s.getRequest();
                if(modReq != null){
                     // We have a modify request; Send modify request to ExoGENI
                    logger.info("CLEANUP: Sending modifyCompute request to ExoGENI...");
                    
                    DisplayPublisher dp;
                    try {
                        dp = new DisplayPublisher(rmProperties);
                        dp.publishInfraMessages(orcaSliceID, "CLEANUP: Sending modifyCompute request (delete node) to ExoGENI...");
                    } catch (Exception ex) {
                        logger.error("Exception while publishing to display exchange");
                    }
                                        
                    String resultModify = orcaManager.sendModifyRequestToORCA(orcaSliceID, modReq);
                    if(resultModify == null){ // Exception
                        logger.error("CLEANUP: could not kill VM in ExoGENI/ORCA");
                    }
                }
            }
            
        }
        
        // Decrease count of numNodestoBeDeleted in RMState
        rmState = RMState.getInstance();
        rmState.decreaseNumNodesToBeDeleted();
        rmState.deleteNodeFromNodesToBeDeletedQ(mnURI);
        logger.info("CLEANUP: decreased numNodestoBeDeleted in RMState");
        logger.info("CLEANUP: current total numNodestoBeDeleted = " + rmState.getNumNodesToBeDeleted());
        
        logger.info("CLEANUP: Finished ...." + orcaSliceID + "|" + mnIP + "|" + mnURI);
        
    }
    
    private void doSSHAndKillCondor(String publicIP) {
        
        String scriptName = rmProperties.getProperty(KILLCONDORONDELETE_SSH_SCRIPTNAME_PROP_NAME);
        if (scriptName == null || scriptName.isEmpty()){
            logger.error("CLEANUP: No condor delete script specified in rm.properties.. can't ssh and stop condor on worker..");
            return;
        }
        
        String pathToSSHPrivKey = rmProperties.getProperty(KILLCONDORONDELETE_SSH_PRIVKEY_PROP_NAME);
        if (pathToSSHPrivKey == null || pathToSSHPrivKey.isEmpty()){
            logger.error("CLEANUP: No ssh priv key path specified in rm.properties.. can't ssh and stop condor on worker..");
            return;
        }
        
        String sshOpts = "-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no";
        String hostName = publicIP;
        
        String sshUserName = rmProperties.getProperty(KILLCONDORONDELETE_SSH_USER_PROP_NAME);
        if(sshUserName == null || sshUserName.isEmpty()){
            sshUserName = "root";
        }
        
        logger.info("CLEANUP: Using " + sshUserName + " user to ssh to " + publicIP);
        String sshCmd = "ssh" + " " + sshOpts + " " + "-i" + " " + pathToSSHPrivKey + " " + sshUserName + "@" + hostName + " " + scriptName;
        logger.info("CLEANUP: sshCmd = \n " + sshCmd);
        
        try {
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec(sshCmd);
 
                BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
 
                String line=null;
 
                while((line=input.readLine()) != null) {
                    logger.info(line);
                }
 
                int exitVal = pr.waitFor();
                logger.info("CLEANUP: ssh command exited with code " + exitVal);
                if(exitVal != 0){
                    logger.error("CLEANUP: Problem ssh-ing and running command in " + sshUserName + "@" + hostName);
                    return;
                }
                else{
                    logger.info("CLEANUP: Successfully ssh-ed and killed condor daemon on " + hostName);
                    return;
                }
 
        } catch(Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            logger.error("CLEANUP: Exception while running ssh command " + e);
            return;
        }        
        
    }    
    
    
}

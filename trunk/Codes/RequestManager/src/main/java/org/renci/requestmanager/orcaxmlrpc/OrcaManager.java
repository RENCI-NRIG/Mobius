/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.orcaxmlrpc;

import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class OrcaManager {
    
    private static Properties rmProperties = null;
    
    private String orcaControllerUrl = null;
    
    protected Logger logger = null;
    
    public OrcaManager(Properties rmProperties){        
        logger = Logger.getLogger(this.getClass());
        this.rmProperties = rmProperties;
        
        if(rmProperties.getProperty(RMConstants.DEFAULT_CONTROLLERURL_PROP) != null){
            orcaControllerUrl = rmProperties.getProperty(RMConstants.DEFAULT_CONTROLLERURL_PROP);
        }
        else{
            orcaControllerUrl = RMConstants.defaultControllerUrl;           
        }
        logger.info("Using ORCA/ExoGENI controller at " + orcaControllerUrl);
    }
    
    /*
    * Sends ndl request to a specific controller
    */
    public String sendCreateRequestToORCA(String sliceId, String controllerUrl, String createReq){

        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = orcaControllerUrl;
        }

        String createRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            createRes = orcaProxy.createSlice(sliceId, createReq);
            logger.info("Result for create slice for " + sliceId + " = " + createRes);
            return createRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA createSlice" + ex);
            return null;
        }

    }

    /*
    * Sends ndl request to the default controller
    */
    public String sendCreateRequestToORCA(String sliceId, String createReq){

        String controllerUrl = orcaControllerUrl;

        String createRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            createRes = orcaProxy.createSlice(sliceId, createReq);
            logger.info("Result for create slice for " + sliceId + " = " + createRes);
            return createRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA createSlice" + ex);
            return null;
        }

    }

    /*
    * Sends ndl modify request to a specific controller
    */
    public String sendModifyRequestToORCA(String sliceId, String controllerUrl, String modifyReq){

        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = orcaControllerUrl;
        }

        String modifyRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            modifyRes = orcaProxy.modifySlice(sliceId, modifyReq);
            logger.info("Result for modify slice for " + sliceId + " = " + modifyRes);
            return modifyRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA modifySlice" + ex);
            return null;
        }

    }

    /*
    * Sends ndl modify request to the default controller
    */
    public String sendModifyRequestToORCA(String sliceId, String modifyReq){

        String controllerUrl = orcaControllerUrl;

        String modifyRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            modifyRes = orcaProxy.modifySlice(sliceId, modifyReq);
            logger.info("Result for modify slice for " + sliceId + " = " + modifyRes);
            return modifyRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA modifySlice" + ex);
            return null;
        }

    }

    /*
    * Sends delete request to the default controller
    */
    public boolean sendDeleteRequestToORCA(String sliceId, String controllerUrl){
        
        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = orcaControllerUrl;
        }

        boolean deleteRes = false;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            deleteRes = orcaProxy.deleteSlice(sliceId);
            logger.info("Result for delete slice for " + sliceId + " = " + deleteRes);
            return deleteRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA deleteSlice" + ex);
            return false;
        }
        
    }
    
    /*
    * Sends delete request to the default controller
    */
    public boolean sendDeleteRequestToORCA(String sliceId){
        
        String controllerUrl = orcaControllerUrl;

        boolean deleteRes = false;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            deleteRes = orcaProxy.deleteSlice(sliceId);
            logger.info("Result for delete slice for " + sliceId + " = " + deleteRes);
            return deleteRes;
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA deleteSlice" + ex);
            return false;
        }
        
    }
    
    /*
    * Gets manifest from default controller
    */               
    public String getManifestFromORCA(String sliceId){

        String controllerUrl = orcaControllerUrl;

        String manifest = null;
        String sanitizedManifest = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            manifest = orcaProxy.sliceStatus(sliceId);
            //logger.info("manifest for slice " + sliceId + " = " + manifest);
            logger.info("Obtained manifest for slice: " + sliceId);
            sanitizedManifest = sanitizeManifest(manifest);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA sliceStatus" + ex);
            return null;
        }
        return sanitizedManifest;

    }

    /*
    * Gets manifest from a specified controller
    */
    public String getManifestFromORCA(String sliceId, String controllerUrl){

        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = orcaControllerUrl;
        }

        String manifest = null;
        String sanitizedManifest = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            manifest = orcaProxy.sliceStatus(sliceId);
            //logger.info("manifest for slice " + sliceId + " = " + manifest);
            logger.info("Obtained manifest for slice: " + sliceId);
            sanitizedManifest = sanitizeManifest(manifest);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA sliceStatus" + ex);
            return null;
        }
        return sanitizedManifest;

    }

    private String sanitizeManifest(String manifest) {

        if (manifest == null)
            return null;

        int ind = manifest.indexOf("<rdf:RDF");
         if (ind > 0)
            return manifest.substring(ind);
         else
            return null;


    }    
    
    
    
}

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
    
    protected Logger logger = null;
    
    public OrcaManager(Properties rmProperties){        
        logger = Logger.getLogger(this.getClass());
        this.rmProperties = rmProperties;        
    }
    
    /*
    * Sends ndl request to a specific controller
    */
    public void sendCreateRequestToORCA(String sliceId, String controllerUrl, String createReq){

        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = RMConstants.defaultControllerUrl;
        }

        String createRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            createRes = orcaProxy.createSlice(sliceId, createReq);
            logger.info("Result for create slice for " + sliceId + " = " + createRes);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA createSlice" + ex);
            return;
        }

    }

    /*
    * Sends ndl request to the default controller
    */
    public void sendCreateRequestToORCA(String sliceId, String createReq){

        String controllerUrl = RMConstants.defaultControllerUrl;

        String createRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            createRes = orcaProxy.createSlice(sliceId, createReq);
            logger.info("Result for create slice for " + sliceId + " = " + createRes);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA createSlice" + ex);
            return;
        }

    }

    /*
    * Sends ndl modify request to a specific controller
    */
    public void sendModifyRequestToORCA(String sliceId, String controllerUrl, String modifyReq){

        if(controllerUrl == null || controllerUrl.isEmpty()){
            controllerUrl = RMConstants.defaultControllerUrl;
        }

        String modifyRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            modifyRes = orcaProxy.modifySlice(sliceId, modifyReq);
            logger.info("Result for modify slice for " + sliceId + " = " + modifyRes);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA modifySlice" + ex);
            return;
        }

    }

    /*
    * Sends ndl modify request to the default controller
    */
    public void sendModifyRequestToORCA(String sliceId, String modifyReq){

        String controllerUrl = RMConstants.defaultControllerUrl;

        String modifyRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            modifyRes = orcaProxy.modifySlice(sliceId, modifyReq);
            logger.info("Result for modify slice for " + sliceId + " = " + modifyRes);
        } catch (Exception ex) {
            logger.error("Exception while calling ORCA modifySlice" + ex);
            return;
        }

    }

    /*
    * Gets manifest from default controller
    */               
    public String getManifestFromORCA(String sliceId){

        String controllerUrl = RMConstants.defaultControllerUrl;

        String manifest = null;
        String sanitizedManifest = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            manifest = orcaProxy.sliceStatus(sliceId);
            logger.info("manifest for slice " + sliceId + " = " + manifest);
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
            controllerUrl = RMConstants.defaultControllerUrl;
        }

        String manifest = null;
        String sanitizedManifest = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            manifest = orcaProxy.sliceStatus(sliceId);
            logger.info("manifest for slice " + sliceId + " = " + manifest);
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

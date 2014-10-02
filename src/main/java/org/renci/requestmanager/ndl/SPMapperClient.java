/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import com.fasterxml.jackson.databind.JsonNode;
import dataModel.NetworkConfiguration;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.renci.requestmanager.RMConstants;
import sslRestAPIClient.NetworkConfigurationImpl;
import sslRestAPIClient.SslRestClient;

/**
 *
 * @author anirban
 */
public class SPMapperClient {
    
    SslRestClient spRegistryAndMapperServiceClient = null;
    Logger logger = null;
    
    public class SPInfo{
        ArrayList<Integer> vlanTagSet = null;
        ArrayList<String> portSet = null;
        ArrayList<String> allowedIPSet = null;
        ArrayList<String> disallowedIPSet = null;
        ArrayList<String> subnetSet = null;
        
        // TODO: Add subnet information

        public SPInfo(){
            
        }
        
        public ArrayList<Integer> getVlanTagSet() {
            return vlanTagSet;
        }

        public void setVlanTagSet(ArrayList<Integer> vlanTagSet) {
            this.vlanTagSet = vlanTagSet;
        }

        public ArrayList<String> getPortSet() {
            return portSet;
        }

        public void setPortSet(ArrayList<String> portSet) {
            this.portSet = portSet;
        }

        public ArrayList<String> getAllowedIPSet() {
            return allowedIPSet;
        }

        public void setAllowedIPSet(ArrayList<String> allowedIPSet) {
            this.allowedIPSet = allowedIPSet;
        }

        public ArrayList<String> getDisallowedIPSet() {
            return disallowedIPSet;
        }

        public void setDisallowedIPSet(ArrayList<String> disallowedIPSet) {
            this.disallowedIPSet = disallowedIPSet;
        }

        public ArrayList<String> getSubnetSet() {
            return subnetSet;
        }

        public void setSubnetSet(ArrayList<String> subnetSet) {
            this.subnetSet = subnetSet;
        }
        
        
        
    }
       
    public SPMapperClient(Logger logger){
        spRegistryAndMapperServiceClient = new SslRestClient();
        this.logger = logger;
    }
    
    public SPInfo getSPInfo(String stitchPortID){
        
        SPInfo spInfo = null;
        
        String stitchPortMapperServiceUrl = querySPRegistry(stitchPortID);
        
        if(stitchPortMapperServiceUrl == null){
            stitchPortMapperServiceUrl = RMConstants.defaultSPMapperUrl;
        }
        
        logger.info("Querying Stitchport Mapper Service URL: " + stitchPortMapperServiceUrl);
       
       //Provided with the Query URL one can query for the network configuration 
        String netConfig = spRegistryAndMapperServiceClient.getNetworkConfigFromURL(stitchPortMapperServiceUrl);
        JsonNode netConfigJson = spRegistryAndMapperServiceClient.getNetworkConfigFromURLInJson(stitchPortMapperServiceUrl);
        
        if(netConfig != null && netConfigJson != null){
            logger.info("Network Configuration for " + stitchPortID + ": " + netConfig);
            
            //Lets build a NetworkConfiguraiton data object 
            NetworkConfiguration myNetworkConfig = new NetworkConfigurationImpl(netConfigJson);
            logger.info("vlanRange " + myNetworkConfig.getVLANRange());
            logger.info("config_url " + myNetworkConfig.getConfigUrl());
            
            
            
            // TODO: get the network properties here and populate spInfo
            // Remove below when we have getters for netConfig contents
            spInfo = new SPInfo();
            
            // Get vlan tag range info
            int firstTag = -1, lastTag = -1;
            logger.info("vlanRange " + myNetworkConfig.getVLANRange());
            String vlanRange = myNetworkConfig.getVLANRange();
            if(vlanRange != null){ 
                if(vlanRange.contains("-")){ // eg. vlanRange = "3208-3212"
                    firstTag = Integer.parseInt(vlanRange.split("-")[0]);
                    lastTag = Integer.parseInt(vlanRange.split("-")[1]);
                }
                else { // eg. vlanRange = "3208"
                    firstTag = Integer.parseInt(vlanRange);
                    lastTag = Integer.parseInt(vlanRange);
                }
                logger.info("firstTag = " + firstTag + " | lastTag = " + lastTag);
            }
            
            ArrayList<Integer> vlanTagSet = new ArrayList<Integer>();
            if(firstTag != -1 && lastTag != -1){ // we have valid vlanRange
                for(int i = firstTag; i <= lastTag; i++){
                    vlanTagSet.add(i);
                }
            }
            else{
                vlanTagSet = null;
            }
            
            // Get port info == config_url
            ArrayList<String> portSet = new ArrayList<String>();
            String config_url = myNetworkConfig.getConfigUrl();
            if(config_url != null){
                portSet.add(config_url);
            }
            else{
                portSet = null;
            }
            
            String firstNetworkOffered =  myNetworkConfig.getAvailableNetworkMasks().get(0);
            logger.info("First network offered : " + firstNetworkOffered);
            logger.info("Available IPs for ntework " + firstNetworkOffered + ":\n "+ myNetworkConfig.getAvailableIPs(myNetworkConfig.getAvailableNetworkMasks().get(0))); 
            logger.info("We only want 20 available IPs for this network: \n"+ myNetworkConfig.getNAvailableIPs(firstNetworkOffered, 20));
            
            ArrayList<String> allowedIPSet = (ArrayList<String>) myNetworkConfig.getAvailableIPs(firstNetworkOffered);
            
            ArrayList<String> subnetSet = (ArrayList<String>) myNetworkConfig.getAvailableNetworkMasks();
            
            // TODO: create a hashMap for allowedIP : {<subnet_i, ip_list_i>} 

            spInfo.setVlanTagSet(vlanTagSet);
            spInfo.setPortSet(portSet);
            spInfo.setAllowedIPSet(allowedIPSet);
            spInfo.setSubnetSet(subnetSet);
            
        }
        else {
            logger.info("Using default SP netConfig properties");
            spInfo = new SPInfo();
            ArrayList<Integer> vlanTagSet = new ArrayList<Integer>();
            vlanTagSet.add(1499);
            ArrayList<String> portSet = new ArrayList<String>();
            portSet.add("http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/4/ethernet");
            ArrayList<String> allowedIPSet = new ArrayList<String>();
            allowedIPSet.add("172.16.1.100/24");
            allowedIPSet.add("172.16.1.101/24");
            ArrayList<String> subnetSet = new ArrayList<String>();
            subnetSet.add("172.16.1.1/24");

            spInfo.setVlanTagSet(vlanTagSet);
            spInfo.setPortSet(portSet);
            spInfo.setAllowedIPSet(allowedIPSet);
            spInfo.setSubnetSet(subnetSet);
            // TODO set subnet
        }               
        
        
        return spInfo;
        
    }
    
    /*
    * Returns the url of the stitchport mapper service from the stitchport registry
    */
    private String querySPRegistry(String stitchPortID){
        
        logger.info("Querying Stitchport Registry to find URL of Stitchport Mapper service for stitchportID = " + stitchPortID);
        String spMapperServiceUrl = spRegistryAndMapperServiceClient.getServiceURL(stitchPortID);
        
        if(spMapperServiceUrl == null){
            logger.error("Did not find SP mapper service url for " + stitchPortID);
            return null;
        }
     
        logger.info("URL of Stitchport Mapper service for " + stitchPortID + " is " + spMapperServiceUrl);
        
        return spMapperServiceUrl;
        
    }
    
}

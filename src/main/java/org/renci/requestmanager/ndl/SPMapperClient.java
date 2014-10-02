/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import com.fasterxml.jackson.databind.JsonNode;
import dataModel.NetworkConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        ArrayList<String> allowedIPSetFirstSubnet = null;
        ArrayList<String> subnetSet = null;
        HashMap<String,List<String>> allIPInfo = null;
        
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

        public ArrayList<String> getAllowedIPSetFirstSubnet() {
            return allowedIPSetFirstSubnet;
        }

        public void setAllowedIPSetFirstSubnet(ArrayList<String> allowedIPSet) {
            this.allowedIPSetFirstSubnet = allowedIPSet;
        }

        public ArrayList<String> getSubnetSet() {
            return subnetSet;
        }

        public void setSubnetSet(ArrayList<String> subnetSet) {
            this.subnetSet = subnetSet;
        }

        public HashMap<String, List<String>> getAllIPInfo() {
            return allIPInfo;
        }

        public void setAllIPInfo(HashMap<String, List<String>> allIPInfo) {
            this.allIPInfo = allIPInfo;
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
            //logger.info("We only want 20 available IPs for this network: \n"+ myNetworkConfig.getNAvailableIPs(firstNetworkOffered, 20));
            
            ArrayList<String> allowedIPSetFirstSubnet = (ArrayList<String>) myNetworkConfig.getAvailableIPs(firstNetworkOffered);
            
            ArrayList<String> subnetSet = (ArrayList<String>) myNetworkConfig.getAvailableNetworkMasks();
            
            HashMap<String, List<String>> ipInfo = (HashMap<String, List<String>>) myNetworkConfig.getIPInfo();
            
            // TODO: create a hashMap for allowedIP : {<subnet_i, ip_list_i>} 

            spInfo.setVlanTagSet(vlanTagSet);
            spInfo.setPortSet(portSet);
            spInfo.setAllowedIPSetFirstSubnet(allowedIPSetFirstSubnet); // ONLY allowedIP set for first subnet
            spInfo.setSubnetSet(subnetSet);
            spInfo.setAllIPInfo(ipInfo);
            
        }
        else {
            logger.info("Using default SP netConfig properties");
            
            spInfo = new SPInfo();
            ArrayList<Integer> vlanTagSet = new ArrayList<Integer>();
            vlanTagSet.add(3208);
            ArrayList<String> portSet = new ArrayList<String>();
            portSet.add("http://geni-orca.renci.org/owl/ion.rdf#ION/Cenic/Cisco/6509/TenGigabitEthernet/1/2/ethernet");
            ArrayList<String> allowedIPSetFirstSubnet = new ArrayList<String>();
            allowedIPSetFirstSubnet.add("10.32.8.20");
            allowedIPSetFirstSubnet.add("10.32.8.22");
            allowedIPSetFirstSubnet.add("10.32.8.22");
            allowedIPSetFirstSubnet.add("10.32.8.23");
            ArrayList<String> subnetSet = new ArrayList<String>();
            subnetSet.add("10.32.8.0/24");

            HashMap<String, List<String>> ipInfo = new HashMap<String, List<String>>();
            ipInfo.put("10.32.8.0/24", allowedIPSetFirstSubnet);
            
            spInfo.setVlanTagSet(vlanTagSet);
            spInfo.setPortSet(portSet);
            spInfo.setAllowedIPSetFirstSubnet(allowedIPSetFirstSubnet);
            spInfo.setSubnetSet(subnetSet);
            spInfo.setAllIPInfo(ipInfo);
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

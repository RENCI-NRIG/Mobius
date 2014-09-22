/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.ndl;

import java.util.Random;
import orca.ndllib.resources.request.*;
import orca.ndllib.Slice;
import orca.ndllib.*;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMConstants;

import org.apache.log4j.Logger;
import org.renci.requestmanager.LinkRequestInfo;

/**
 *
 * @author anirban
 */
public class NdlLibManager implements RMConstants{
    
    // This is the class responsible for talking to ndlLib and generate requests
    
    public NdlLibManager(){
        
    }
    
    public String generateNewCondorRequest(NewRequestInfo newReq, Logger logger){
        
        // Parse what fields are present in newReq and generate appropriate ndl request
        Slice s = new Slice();
        String templateType = newReq.getTemplateType();
        logger.debug("Start generating ndl request for request type : " + templateType);

        // Basic master-worker topology for Condor Pool
        ComputeNode master     = s.addComputeNode("Master");
        ComputeNode workers    = s.addComputeNode("Workers");
        BroadcastNetwork net   = s.addBroadcastLink("Network");

        InterfaceNode2Net masterIface  = (InterfaceNode2Net) net.stitch(master);
        InterfaceNode2Net workersIface = (InterfaceNode2Net) net.stitch(workers);

        // Find if user requested particular image
        // if any of the image attibutes is null, use default image
        if(newReq.getNewImageUrl() == null || newReq.getNewImageHash() == null || newReq.getNewImageName() == null){ 
            logger.info("Using image: " + CondorDefaults.getDefaultImageUrl() + " | " + CondorDefaults.getDefaultImageHash() + " | " + CondorDefaults.getDefaultImageName());
            master.setImage(CondorDefaults.getDefaultImageUrl(), CondorDefaults.getDefaultImageHash(), CondorDefaults.getDefaultImageName());            
            workers.setImage(CondorDefaults.getDefaultImageUrl(), CondorDefaults.getDefaultImageHash(), CondorDefaults.getDefaultImageName()); 
        }
        else {
            logger.info("Using image: " + newReq.getNewImageUrl() + " | " + newReq.getNewImageHash()+ " | " + newReq.getNewImageName());
            master.setImage(newReq.getNewImageUrl(), newReq.getNewImageHash(), newReq.getNewImageName());
            workers.setImage(newReq.getNewImageUrl(), newReq.getNewImageHash(), newReq.getNewImageName());
        }
        
        // Get a domain randomly from the set of available domains
        int numDomains = ComputeDomains.values().length;
        int pick = new Random().nextInt(numDomains);
        String domainName1 = ComputeDomains.values()[pick].name;
        String domainName2 = ComputeDomains.values()[(pick+1)%numDomains].name;
        
        // Choose domains for master and worker
        if(templateType.matches(MultiSuffix)){ // master and workers requested to be in separate domains
            logger.info("Using master domain = " + domainName1);
            logger.info("Using workers domain = " + domainName2);
            master.setDomain(domainName1);
            workers.setDomain(domainName2);
        }
        else { // master and worker in the same domain
            logger.info("Using master domain = " + domainName1);
            logger.info("Using workers domain = " + domainName1);
            master.setDomain(domainName1);
            workers.setDomain(domainName1);
        }
        
        // Choose postbootscript for master
        if(newReq.getNewPostbootMaster() == null){ // No postboot script supplied by user; use default postboot script
            logger.info("Using default postboot script for master");
            master.setPostBootScript(CondorDefaults.getDefaultPostbootMaster());
        }
        else {
            logger.info("Using user-supplied postboot script for master");
            master.setPostBootScript(newReq.getNewPostbootMaster());
        }
        
        // Choose postbootscript for workers
        if(newReq.getNewPostbootWorker() == null){ // No postboot script supplied by user; use default postboot script
            logger.info("Using default postboot script for workers");
            master.setPostBootScript(CondorDefaults.getDefaultPostbootWorker());
        }
        else {
            logger.info("Using user-supplied postboot script for workers");
            master.setPostBootScript(newReq.getNewPostbootWorker());
        }
        
        // Choose number of workers
        if(newReq.getNewCompRes() <= 0){
            logger.info("Using default number of workers = " + CondorDefaults.getDefaultNumWorkers());
            workers.setNodeCount(CondorDefaults.getDefaultNumWorkers());    
        }
        else{
            logger.info("Using user-supplied number of workers = " + newReq.getNewCompRes());
            workers.setNodeCount(newReq.getNewCompRes());
        }
            
        // Choose bandwidth between compute resources
        if(newReq.getNewBandwidth() <= 0){
            logger.info("Using default bandwidth between compute resources = " + CondorDefaults.getDefaultBW());
            net.setBandwidth(CondorDefaults.getDefaultBW());
        }
        else{
            logger.info("Using user-supplied bandwidth between compute resources = " + newReq.getNewBandwidth());
            net.setBandwidth(newReq.getNewBandwidth());
        }
        
        // Choose instance type
        master.setNodeType("XO Large");
        workers.setNodeType("XO Large");
        
        // At this point basic condor cluster is ready, without IP address assignment
        
        // Now add storage or stitchports, if either is requested
        
        // Storage: TODO: Ask Paul how to do this
        if(templateType.matches(StorageSuffix)){ //Storage is requested
            // Create new storage node and attach to master node
            StorageNode storage = s.addStorageNode("Storage");
            Network link = s.addLink("LinkToStorage");
            storage.stitch(link);
            // Set properties of storage
        }
        
        // Stitchport + autoIP
        if(templateType.matches(SPSuffix)){ // Stitchport requested
            StitchPort  data       = s.addStitchPort("Data");
            InterfaceNode2Net dataIface    = (InterfaceNode2Net) net.stitch(data);
            
            // TODO: Read LinkRequestInfo inside newReq; query SP mapper and get these properties
            
            LinkRequestInfo linkReq = newReq.getNewLinkInfo();
            if(linkReq != null){
                String stitchPortID = linkReq.getStitchPortID();
                
                SPMapperClient spMapperClient = new SPMapperClient();
                SPMapperClient.SPInfo spInfo = spMapperClient.getSPInfo(stitchPortID);
                
                int label = spInfo.getVlanTagSet().get(0).intValue(); // get the first available vlan tag
                String port = spInfo.getPortSet().get(0);
                data.setLabel(Integer.toString(label)); //
                data.setPort(port);
                
                // set bandwidth with stitchport bandwidth if that is more then the bandwidth of broadcast network                
                if(linkReq.getLinkBandwidth() > 0 && linkReq.getLinkBandwidth() > net.getBandwidth()){
                    logger.info("Using user-supplied link to SP bandwidth as the slice bandwidth");
                    net.setBandwidth(linkReq.getLinkBandwidth());
                }
                
                // TODO: put constraints on auto-IP
                // subnet = spInfo.getSubnet();
                // net.setSubnet(subnet);
                // availIP = spInfo.getAllowedIP();
                // net.setAvailIP(availIP);
                                
                
            }
            // TODO: Call auto-IP
            // s.sutoIP();
        }
        else{ // No SP requested
            // TODO: Call auto-IP
            // s.sutoIP();
        }

        // TODO: Call auto-IP
        masterIface.setIpAddress("172.16.1.1");
        masterIface.setNetmask("255.255.255.0");
        workersIface.setIpAddress("172.16.1.100");
        workersIface.setNetmask("255.255.255.0");
        
        logger.debug("Generated request = " + "\n" + s.getRequest());
        
        return (s.getRequest());        

    }
    
    public String generateNewHadoopRequest(NewRequestInfo newReq){
        return null;
    }
    
    public String generateNewMPIRequest(NewRequestInfo newReq){
        return null;
    }
    
    
        public String generateTestRequest(){
        
        Slice s = new Slice();
        s.logger().debug("adamantTest1: ");

        ComputeNode master     = s.addComputeNode("Master");
        ComputeNode workers    = s.addComputeNode("Workers");
        //StitchPort  data       = s.addStitchPort("Data");
        BroadcastNetwork net   = s.addBroadcastLink("Network");

        InterfaceNode2Net masterIface  = (InterfaceNode2Net) net.stitch(master);
        InterfaceNode2Net workersIface = (InterfaceNode2Net) net.stitch(workers);
        //InterfaceNode2Net dataIface    = (InterfaceNode2Net) net.stitch(data);

        master.setImage("http://geni-images.renci.org/images/standard/centos/centos6.3-v1.0.11.xml","776f4874420266834c3e56c8092f5ca48a180eed","PRUTH-centos");
        master.setNodeType("XO Large");
        master.setDomain("RENCI (Chapel Hill, NC USA) XO Rack");
        master.setPostBootScript("master post boot script");

        masterIface.setIpAddress("172.16.1.1");
        masterIface.setNetmask("255.255.255.0");

        workers.setImage("worker_url", "worker_hash", "worker_shortName");
        workers.setImage("http://geni-images.renci.org/images/standard/centos/centos6.3-v1.0.11.xml","776f4874420266834c3e56c8092f5ca48a180eed","PRUTH-centos");
        workers.setNodeType("XO Large");
        workers.setDomain("RENCI (Chapel Hill, NC USA) XO Rack");
        workers.setPostBootScript("worker post boot script");
        workers.setNodeCount(2);

        workersIface.setIpAddress("172.16.1.100");
        workersIface.setNetmask("255.255.255.0");

        //data.setLabel("1499");
        //data.setPort("http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/4/ethernet");

        s.logger().debug("******************** START REQUEST *********************");
        s.logger().debug(s.getRequestString());
        System.out.println(s.getRequest());
        s.logger().debug("******************** END REQUEST *********************");
        return (s.getRequest());
        
        
    }
    
    
}

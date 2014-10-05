/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.ndl;

import java.util.ArrayList;
import java.util.Random;
import orca.ndllib.resources.request.*;
import orca.ndllib.Slice;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMConstants;

import org.apache.log4j.Logger;
import org.renci.requestmanager.LinkRequestInfo;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.policy.IModifyPolicy;
import org.renci.requestmanager.policy.SimpleUnitsModifyPolicy;

/**
 *
 * @author anirban
 */
public class NdlLibManager implements RMConstants{
    
    // This is the class responsible for talking to ndlLib and generate requests
    protected Logger logger = null;
    
    
    public NdlLibManager(){
        logger = Logger.getLogger(this.getClass());
    }
    
    public String generateNewCondorRequest(NewRequestInfo newReq){
        
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
        if(templateType.contains(MultiSuffix)){ // master and workers requested to be in separate domains
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
            workers.setPostBootScript(CondorDefaults.getDefaultPostbootWorker());
        }
        else {
            logger.info("Using user-supplied postboot script for workers");
            workers.setPostBootScript(newReq.getNewPostbootWorker());
        }
        
        // Choose number of workers, max nodecount 
        if(newReq.getNewCompRes() <= 0){
            logger.info("Using default number of workers = " + CondorDefaults.getDefaultNumWorkers());
            workers.setNodeCount(CondorDefaults.getDefaultNumWorkers());
        }
        else{
            logger.info("Using user-supplied number of workers = " + newReq.getNewCompRes());
            workers.setNodeCount(newReq.getNewCompRes());            
        }
        
        workers.setMaxNodeCount(CondorDefaults.getDefaultMaxNumWorkers());
        
        master.setNodeCount(1);
        master.setMaxNodeCount(1);
            
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
        workers.setNodeType("XO Medium");
        
        // At this point basic condor cluster is ready, without IP address assignment
        
        // Now add storage or stitchports, if either is requested
        
        // Storage: TODO: Ask Paul how to do this
        if(templateType.contains(StorageSuffix)){ //Storage is requested
            // Create new storage node and attach to master node
            // TODO: Handle storage
            StorageNode storage = s.addStorageNode("Storage");
            Network link = s.addLink("LinkToStorage");
            storage.stitch(link);
            // Set properties of storage
        }
        
        
        // Stitchport + autoIP
        if(templateType.contains(SPSuffix)){ // Stitchport requested
                        
            logger.info("request includes request for stitchport");
            // TODO: Read LinkRequestInfo inside newReq; query SP mapper and get these properties
            
            LinkRequestInfo linkReq = newReq.getNewLinkInfo();
            if(linkReq != null){
                String stitchPortID = linkReq.getStitchPortID();
                
                SPMapperClient spMapperClient = new SPMapperClient(logger);
                SPMapperClient.SPInfo spInfo = spMapperClient.getSPInfo(stitchPortID);
                
                if(spInfo != null){
                    int label = spInfo.getVlanTagSet().get(0); // get the first available vlan tag
                    String port = spInfo.getPortSet().get(0);

                    StitchPort  data       = s.addStitchPort("Data");
                    InterfaceNode2Net dataIface    = (InterfaceNode2Net) net.stitch(data);

                    logger.info("Using stitchport vlan tag = " + label);
                    logger.info("Using stitchport port urn = " + port);

                    data.setLabel(Integer.toString(label)); //
                    data.setPort(port);

                    // set bandwidth with stitchport bandwidth if that is more then the bandwidth of broadcast network                
                    if(linkReq.getLinkBandwidth() > 0 && linkReq.getLinkBandwidth() > net.getBandwidth()){
                        logger.info("Using user-supplied link to SP bandwidth as the slice bandwidth");
                        net.setBandwidth(linkReq.getLinkBandwidth());
                    }

                    // TODO: put constraints on auto-IP
                    ArrayList<String> subnetSet = spInfo.getSubnetSet();
                    String firstSubnet = subnetSet.get(0); // "10.32.8.0/24"
                    String firstSubnetNetwork = firstSubnet.split("/")[0]; // "10.32.8.0"
                    String firstSubnetMask = firstSubnet.split("/")[1]; // "24"
                    logger.info("passing " + firstSubnetNetwork + " and " + firstSubnetMask + " to ndllib's setIPSubnet");
                    net.setIPSubnet(firstSubnetNetwork, Integer.parseInt(firstSubnetMask));
                    
                    net.clearAvailableIPs(); 
                    ArrayList<String> availIPSet = spInfo.getAllowedIPSetFirstSubnet();
                    logger.info(" passing to ndllib as available IPs: " + availIPSet);
                    for (String ip: availIPSet){
                        net.addAvailableIP(ip);
                    }
                    
                    logger.info("availIPSet size = " + availIPSet.size());
                    // TODO: Fix the argument to setMaxNodeCount: (availIPSet.size() - 1 ) is the correct one
                    workers.setMaxNodeCount(availIPSet.size() - 2);
                    //workers.setMaxNodeCount(5);
                    
                    // NOTE: If we have to deal with multiple subnets and allowed ips in future, we have that info in spInfo.getAllIPInfo()
                    
                }              
                
            }
            
            s.autoIP();
        }
        else{ // No SP requested
            s.autoIP();
        }
        
        logger.debug("Generated request = " + "\n" + s.getRequest());
        
        // For debugging saving the request to a file in /tmp
        s.save("/tmp/generatedRequest.rdf");
        
        return (s.getRequest());        

    }
    
    public String generateNewHadoopRequest(NewRequestInfo newReq){
        return null;
    }
    
    public String generateNewMPIRequest(NewRequestInfo newReq){
        return null;
    }
    
    public String generateModifyComputeRequest(ModifyRequestInfo modReq, String currManifest) {
        
        Slice s = new Slice();
        s.loadRDF(currManifest);
        
        logger.debug("SliceState = " + s.getState());
        
        ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
        if(cn == null){
            logger.error("Manifest doesn't have a Nodegroup named Workers, can't modify Nodegroup");
            return null;
        }
        
        // Run policy to determine change in number of workers
        IModifyPolicy modPolicy = new SimpleUnitsModifyPolicy(logger);
        int change = modPolicy.determineChangeInNumWorkers(modReq, currManifest);
        
        if(change == 0){
            logger.info("Modify policy determined that no new nodes should be added, and no existing node should be deleted");
            return null;
        }        
        
        if(change < 0){ // Need to delete some workers           
            int i = 0;
            int numNodesToDelete = (-1)*change;
            for (orca.ndllib.resources.manifest.Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(i < numNodesToDelete){
                    logger.info("manifestNode: deleting " + mn.getURI());
                    mn.delete();
                    i++;
                }
            }
        }
        
        if (change > 0){ // Need to add more workers
            int numWorkers = 0;
            for (orca.ndllib.resources.manifest.Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                numWorkers++;
            }
            logger.info("There are " + numWorkers + "worker nodes in the current manifest");
            int newNumWorkers = numWorkers + change;
            logger.info("Making new size of Workers nodegroup to " + newNumWorkers + " by adding " + change + " workers");
            cn.setNodeCount(newNumWorkers);            
        }     
        
        s.save("/tmp/generatedModifyRequest.rdf");
        return s.getRequest();
    }   
    
    public String getSliceManifestStatus(String manifest){
        Slice s = new Slice();
        s.loadRDF(manifest);
        return s.getState();
    }
    
    public int getNumWorkersInManifest(String manifest){
        
        Slice s = new Slice();
        s.loadRDF(manifest);
        ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
        if(cn == null){
            logger.error("Manifest doesn't have a Nodegroup named Workers..");
            return -1;
        }
        int numWorkers = 0;
        for (orca.ndllib.resources.manifest.Node mn : ((ComputeNode)cn).getManifestNodes()){
            logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
            numWorkers++;
        }
        logger.info("There are " + numWorkers + " worker nodes in the current manifest");
        return numWorkers;
        
    }
    
    // Qn: if some are in failed state, what is the overall sliceState ?
    
    public int getNumActiveWorkersInManifest(String manifest){
        
        Slice s = new Slice();
        s.loadRDF(manifest);
        ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
        if(cn == null){
            logger.error("Manifest doesn't have a Nodegroup named Workers..");
            return -1;
        }
        int numActiveWorkers = 0;
        for (orca.ndllib.resources.manifest.Node mn : ((ComputeNode)cn).getManifestNodes()){
            logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
            if(!mn.getState().equalsIgnoreCase("Active")){
                numActiveWorkers++;
            }
        }
        logger.info("There are " + numActiveWorkers + " Active worker nodes in the current manifest");
        return numActiveWorkers;
        
    }
    
    public boolean areAllWorkersInManifestActive(String manifest){
        
        Slice s = new Slice();
        s.loadRDF(manifest);
        ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
        if(cn == null){
            logger.error("Manifest doesn't have a Nodegroup named Workers..");
            return false;
        }
        
        for (orca.ndllib.resources.manifest.Node mn : ((ComputeNode)cn).getManifestNodes()){
            logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
            if(!mn.getState().equalsIgnoreCase("Active")){
                return false;
            }
        }
        // Code gets here when all are "Active"
        return true;
        
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

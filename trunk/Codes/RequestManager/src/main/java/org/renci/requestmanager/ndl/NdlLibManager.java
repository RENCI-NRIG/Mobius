/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.ndl;

import orca.ndllib.resources.request.*;
import orca.ndllib.Slice;
import orca.ndllib.*;

/**
 *
 * @author anirban
 */
public class NdlLibManager {
    
    // This is the class responsible for talking to ndlLib and generate requests
    
    public NdlLibManager(){
        
    }
    
    public String generateRequest(){
        
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

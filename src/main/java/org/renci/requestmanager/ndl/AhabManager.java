/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import com.google.common.collect.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.extras.PriorityNetwork;
import org.renci.ahab.libndl.resources.manifest.Node;
import org.renci.ahab.libndl.resources.request.BroadcastNetwork;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.Interface;
import org.renci.ahab.libndl.resources.request.StitchPort;
import org.renci.ahab.libndl.resources.request.StorageNode;
import org.renci.ahab.libtransport.*;
import org.renci.ahab.libtransport.util.ContextTransportException;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.util.TransportException;
import org.renci.ahab.libtransport.util.UtilTransportException;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;
import org.renci.requestmanager.LinkRequestInfo;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMConstants;
import static org.renci.requestmanager.RMConstants.PREFERRED_DOMAINS_STRING_NAME;
import org.renci.requestmanager.RMState;

/**
 *
 * @author anirban
 */
public class AhabManager implements RMConstants{
    
        // This is the class responsible for talking to ahab and generate requests
        protected Logger logger = null;
        private static Properties rmProperties = null; // need this for preferred domains
        
        protected RMState rmState = null;


        public AhabManager(Properties rmProps){ // constructor required for create requests for getting preferred domains from configuration file
            logger = Logger.getLogger(this.getClass());
            this.rmProperties = rmProps;
        }

        public AhabManager(){
            logger = Logger.getLogger(this.getClass());
        }

        
        public String processModifyNetworkTest(ModifyRequestInfo modReq, String orcaSliceID){
            
            String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
            String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);
            
            ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);
            String sliceName =  orcaSliceID;
            
            Slice currSlice = getSlice(sliceProxy, sliceName);
            
            if(currSlice == null){
                logger.error("processModifyNetwork: Couldn't get AHAB Slice object for ORCA slice: " + orcaSliceID);
                return "ERROR";
            }
            
            currSlice.refresh();
            PriorityNetwork sdn = PriorityNetwork.get(currSlice, "PegasusHTCondorSDX");
            sdn.QoS_setDefaultPriority(10);
            
            String sourceEndPoint = modReq.getEndpointSrc();
            String destEndPoint = modReq.getEndpointDst();
            int flowPriority = modReq.getFlowPriority();
            
            //sdn.QoS_setPriority(sourceEndPoint, destEndPoint, flowPriority);
            sdn.QoS_setPriority("S0", destEndPoint, flowPriority);
            sdn.QoS_setPriority("S0", "S2", 20);
            sdn.QoS_commit();
            
            return "SUCCESS";
            
        }

        
        public String processModifyNetwork(String orcaSliceID){
            
            
            logger.info("AhabManager: Called processModifyNetwork()...");
            
            String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
            String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);
            
            ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);
            String sliceName =  orcaSliceID;
            
            Slice currSlice = getSlice(sliceProxy, sliceName);
            
            if(currSlice == null){
                logger.error("processModifyNetwork: Couldn't get AHAB Slice object for ORCA slice: " + orcaSliceID);
                return "ERROR";
            }
            
            currSlice.refresh();
            PriorityNetwork sdn = PriorityNetwork.get(currSlice, "PegasusHTCondorSDX");
            
            logger.info("AhabManager: processModifyNetwork(): Setting default QoS priority...");
            sdn.QoS_setDefaultPriority(10);
            
            // Go through all <src, dst, priority> tuples in crossSitePriority in RMState and set priorities in the priority network
            
            rmState = RMState.getInstance();
            
            Table<String, String, String> crossSiteFlowPriorityMap = rmState.getCrossSitePriority();
            for (Map.Entry<String, Map<String,String>> outer : crossSiteFlowPriorityMap.rowMap().entrySet()) {
                
                for (Map.Entry<String, String> inner : outer.getValue().entrySet()) {
                    
                    String endPointSrc = outer.getKey();
                    String endPointDst = inner.getKey();
                    String flowPriority = inner.getValue();
                    
                    logger.info("AhabManager: processModifyNetwork(): Setting QoS priority between " + endPointSrc + " and " + endPointDst + " as " + Integer.parseInt(flowPriority));
                    
                    // TODO: remove this hack after resolving naming of data node
                    if(endPointSrc.equalsIgnoreCase("data")){
                        endPointSrc = "S0";
                    }
                    
                    sdn.QoS_setPriority(endPointSrc, endPointDst, Integer.parseInt(flowPriority));
                    
                }
            }
            
            sdn.QoS_commit();
            
            return "SUCCESS";
            
        }        
        
        
        /*
        This creates a basic PegasusHTCondorSDX slice
        */
        public String processNewSDXCondor(NewRequestInfo newReq, String orcaSliceID){
        
                System.out.println("processNewSDXCondor: START");
                
                // TODO: Get the names of the prongs from rmProperties; Those will be the names that would be used while generating DAX

		//String pemLocation = "/Users/anirban/.ssl/geni-anirban.pem";
                //String sshKeyLocation = "/Users/anirban/.ssh/id_rsa.pub";
		//String controllerUrl = "https://geni.renci.org:11443/orca/xmlrpc";
                
                String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
                String sshKeyLocation = rmProperties.getProperty(USER_SSHKEY_PATH_PROP);
                String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);
                
		String sliceName =  orcaSliceID;


		ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);		

		//SSH context
		SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
		try {
			SSHAccessTokenFileFactory fac;
			fac = new SSHAccessTokenFileFactory(sshKeyLocation, false);

			SSHAccessToken t = fac.getPopulatedToken();			
			sctx.addToken("root", "root", t);
			sctx.addToken("root", t);
		} catch (UtilTransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//Main Example Code
		
		Slice s = Slice.create(sliceProxy, sctx, sliceName);
		
		//PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX"); // billion would be a gigabit; this is overall bandwidth of the slice
                
                // Put OpenFlowSDXController VM at WVN and 1 Gigabit
                //PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX", "WVN (UCS-B series rack in Morgantown, WV, USA)", 1000000000); 
                //PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX", "WVN (UCS-B series rack in Morgantown, WV, USA)", 500000000);
                PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX", rmProperties.getProperty(SDX_DEFAULT_SDXCONTROLLER_DOMAIN_PROP), 500000000);
                
                ArrayList<String> preferredDomains = populatePreferredDomains(rmProperties);
                logger.info("Preferred set of domains = " + preferredDomains);

                int siteCount = preferredDomains.size();
		for (int i = 0; i <siteCount; i++){
			sdn.bind("S"+i,preferredDomains.get(i));
		}
                
                s.commit(10,10);
                
                sleep(30);
                
                while(true){
                                           
			s.refresh();
	
			sdn = PriorityNetwork.get(s,"PegasusHTCondorSDX");
		
			System.out.println("Fetched sdn: " + s.getAllResources());
			if(sdn.getState().equals("Active")){
				System.out.println("All Active!");
				break;
			}
			sleep(siteCount*5);
		}
		

                
                SDXHTCondorDefaults d = new SDXHTCondorDefaults();
                
                //String nodeImageShortName="genome-0.2";
		//String nodeImageURL ="http://geni-images.renci.org/images/anirban/panorama/genome-0.2/genome-0.2.xml";
		//String nodeImageHash ="bff2c4eef5ebfc0713e781df7bd0ae26851381a1";
		//String nodeNodeType="XO Extra large";
		//String nodePostBootScript="/bin/date > /tmp/date.out";
                
                String nodeImageShortName = d.getDefaultImageName();
                String nodeImageURL = d.getDefaultImageUrl();
                String nodeImageHash = d.getDefaultImageHash();
                String nodeNodeType = d.getDefaultImageType();
                String nodePostBootScript = null;
                

		for (int i = 0; i < siteCount; i++){
                        
                        String nodeDomain=preferredDomains.get(i);			
			
                        if(i == 0){ // data site condition
                            
                            String newDataNodeName = "data";
                            
                            String dataNodeImageURL = d.getDefaultDataImageUrl();
                            String dataNodeImageHash = d.getDefaultDataImageHash();
                            String dataNodeImageShortName = d.getDefaultDataImageName();
                            String dataNodeNodeType = d.getDefaultImageType();
                            String dataNodePostBootScript = d.getPostbootDataNode();
                            
                            ComputeNode dataNode = s.addComputeNode(newDataNodeName);
                            dataNode.setImage(dataNodeImageURL,dataNodeImageHash,dataNodeImageShortName);
                            dataNode.setNodeType(dataNodeNodeType);
                            dataNode.setDomain(nodeDomain);
                            dataNode.setPostBootScript(dataNodePostBootScript);
                            
                            String dataIP = "172.16.1.200";
                            String dataSite = "S"+i;
                            //String dataSite = "data";
                            
                            sdn.addNode(dataNode,dataSite,dataIP,"255.255.0.0");
                            
                        }
                        else { // condor pool 
                            for (int j = 0; j < (1 + SDXHTCondorDefaults.getDefaultNumWorkers()); j++){ // loop through (master + workers) ; 0th is master rest are workers
                                                                    
                                    String newNodeName = null;
                                    
                                    if(j == 0){
                                        newNodeName = "master"+i;
                                    }
                                    else{
                                        newNodeName = "workers"+i+"-"+(j-1);
                                    }
                                    
                                    ComputeNode node = s.addComputeNode(newNodeName);
                                    node.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
                                    node.setNodeType(nodeNodeType);
                                    node.setDomain(nodeDomain);
                                    

                                    String ip = null;
                                    if(j == 0) { // master
                                        ip = "172.16."+i+"."+"1";
                                        nodePostBootScript = d.getPostbootMaster(i);
                                    }
                                    else{ // workers
                                        ip = "172.16."+i+"."+(100+(j-1));
                                        nodePostBootScript = d.getPostbootWorker(i);
                                    }

                                    node.setPostBootScript(nodePostBootScript);
                                    
                                    String site = "S"+i;
                                    
                                    sdn.addNode(node,site,ip,"255.255.0.0"); //"255.255.0.0"
                            }
                        }
                        
		}
                
//                String nodeImageShortName="Centos6.7-SDN.v0.1";
//		String nodeImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
//		String nodeImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
//		String nodeNodeType="XO Medium";
//		String nodePostBootScript="switch boot script";
//
//                for (int i = 0; i < siteCount; i++){
//                        
//                        String nodeDomain=preferredDomains.get(i);			
//			
//			for (int j = 0; j < 2; j++){
//				String newNodeName = "node"+i+"-"+j;
//
//				ComputeNode node = s.addComputeNode(newNodeName);
//				node.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
//				node.setNodeType(nodeNodeType);
//				node.setDomain(nodeDomain);
//				node.setPostBootScript(nodePostBootScript);
//
//				String site = "S"+i;
//				String ip = "172.16."+i+"."+(100+j);
//
//				sdn.addNode(node,site,ip);
//			}
//		}
                
                
		s.commit(10,10);
                
                return "SUCCESS";
                
	}
        
        public String processNewSDXCondorTest(NewRequestInfo newReq, String orcaSliceID){
        
                System.out.println("processNewSDXCondor: START");

		//String pemLocation = "/Users/anirban/.ssl/geni-anirban.pem";
                //String sshKeyLocation = "/Users/anirban/.ssh/id_rsa.pub";
		//String controllerUrl = "https://geni.renci.org:11443/orca/xmlrpc";
                
                String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
                String sshKeyLocation = rmProperties.getProperty(USER_SSHKEY_PATH_PROP);
                String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);
                
		String sliceName =  orcaSliceID;


		ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);		

		//SSH context
		SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
		try {
			SSHAccessTokenFileFactory fac;
			fac = new SSHAccessTokenFileFactory(sshKeyLocation, false);

			SSHAccessToken t = fac.getPopulatedToken();			
			sctx.addToken("root", "root", t);
			sctx.addToken("root", t);
		} catch (UtilTransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//Main Example Code
		
		Slice s = Slice.create(sliceProxy, sctx, sliceName);
		
		String controllerSite = "WVN (UCS-B series rack in Morgantown, WV, USA)";
		PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX", controllerSite);
                
                ArrayList<String> preferredDomains = populatePreferredDomains(rmProperties);
                logger.info("Preferred set of domains = " + preferredDomains);

                int siteCount = preferredDomains.size();
		for (int i = 0; i <siteCount; i++){
			sdn.bind("S"+i,preferredDomains.get(i));
		}
                
                s.commit(10,10);
                
                while(true){
			s.refresh();
	
			sdn = PriorityNetwork.get(s,"PegasusHTCondorSDX");
		
			System.out.println("Fetched sdn: " + s.getAllResources());
			if(sdn.getState().equals("Active")){
				System.out.println("All Active!");
				break;
			}
			sleep(siteCount*5);
		}
		
                
                String nodeImageShortName="Centos6.7-SDN.v0.1";
		String nodeImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
		String nodeImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
		String nodeNodeType="XO Medium";
		String nodePostBootScript="switch boot script";

                for (int i = 0; i < siteCount; i++){
                        
                        String nodeDomain=preferredDomains.get(i);			
			
			for (int j = 0; j < 2; j++){
				String newNodeName = "node"+i+"-"+j;

				ComputeNode node = s.addComputeNode(newNodeName);
				node.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
				node.setNodeType(nodeNodeType);
				node.setDomain(nodeDomain);
				node.setPostBootScript(nodePostBootScript);

				String site = "S"+i;
				String ip = "172.16."+i+"."+(100+j);

				sdn.addNode(node,site,ip,"255.255.0.0");
			}
		}
                
                
		s.commit(10,10);
                
                return "SUCCESS";
                
	}
        

	public static Slice getSlice(ISliceTransportAPIv1 sliceProxy, String sliceName){
		Slice s = null;
		try {
			s = Slice.loadManifestFile(sliceProxy, sliceName);
		} catch (ContextTransportException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return s;
	}


	public static ISliceTransportAPIv1 getSliceProxy(String pem, String controllerUrl){

		ISliceTransportAPIv1 sliceProxy = null;
		try{
			//ExoGENI controller context
			ITransportProxyFactory ifac = new XMLRPCProxyFactory();
			System.out.println("Opening certificate " + pem + " and key " + pem);
			TransportContext ctx = new PEMTransportContext("", pem, pem);
			sliceProxy = ifac.getSliceProxy(ctx, new URL(controllerUrl));

		} catch  (Exception e){
			e.printStackTrace();
			System.err.println("Proxy factory test failed");
			assert(false);
		}

		return sliceProxy;
	}
        
 



	public static final ArrayList<String> domains;
	static {
		ArrayList<String> l = new ArrayList<String>();

		for (int i = 0; i < 10; i++){
			l.add("PSC (Pittsburgh, TX, USA) XO Rack");

			//l.add("RENCI (Chapel Hill, NC USA) XO Rack");
			//l.add("TAMU (College Station, TX, USA) XO Rack");
			//l.add("UH (Houston, TX USA) XO Rack");
			
			
			//l.add("WSU (Detroit, MI, USA) XO Rack");
			//l.add("UFL (Gainesville, FL USA) XO Rack");
			//l.add("OSF (Oakland, CA USA) XO Rack");
			//l.add("SL (Chicago, IL USA) XO Rack");
			//l.add("UMass (UMass Amherst, MA, USA) XO Rack");
			//l.add("WVN (UCS-B series rack in Morgantown, WV, USA)");
			//l.add("UAF (Fairbanks, AK, USA) XO Rack");
			//l.add("BBN/GPO (Boston, MA USA) XO Rack");
			//l.add("UvA (Amsterdam, The Netherlands) XO Rack");

		}

		domains = l;
	}
        
        private ArrayList<String> populatePreferredDomains(Properties rmProps){

            ArrayList<String> listPreferredDomains = new ArrayList<String> ();
            if(rmProps == null){
                logger.error("While populating preferredDomains from rmProperties, rmProperties turned out to be null");
                return null;
            }

            if(rmProps.getProperty(PREFERRED_DOMAINS_STRING_NAME) == null){
                logger.info("No property called preferredDomains found in rm.properties, .. returning null for preferredDomains..");
                return null;
            }
            else{ // We have some preferred domains
                String allPreferredDomainsString = rmProps.getProperty(PREFERRED_DOMAINS_STRING_NAME);
                StringTokenizer st = new StringTokenizer(allPreferredDomainsString, ";"); 
                while(st.hasMoreTokens()) {  
                    String val = st.nextToken(); 
                    listPreferredDomains.add(val);
                } 
                return listPreferredDomains;
            }

        }
        
        
    	public static void sleep(int sec){
		try {
		    Thread.sleep(sec*1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {  
		    Thread.currentThread().interrupt();
		}
	}
	
	
	public static void blockUntilUp(String pem, String sliceName, String nodeName){
            String SDNControllerIP = null; 
            while (SDNControllerIP == null){

                SDNControllerIP=AhabManager.getPublicIP(pem, sliceName, nodeName);
                System.out.println(nodeName + " SDNControllerIP: " + SDNControllerIP);

                if(SDNControllerIP != null) break;

                AhabManager.sleep(30);
            };
	}
	
        /*
        Get public IP for a specific node in a slice
        */
	public static String getPublicIP(String pem, String sliceName, String nodeName){		
		
		try{

			//ExoGENI controller context
			ITransportProxyFactory ifac = new XMLRPCProxyFactory();
			System.out.println("Opening certificate " + pem + " and key " + pem);
			TransportContext ctx = new PEMTransportContext("", pem, pem);
			ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
		
				
			Slice s = Slice.loadManifestFile(sliceProxy, sliceName);
	
		
			//System.out.println("Slice: " + s.getDebugString());
			//System.out.println("Slice: " + s.getSliceGraphString());
			ComputeNode  node = (ComputeNode) s.getResourceByName(nodeName);
				
			//System.out.println("node: " + node);
			
			//InterfaceNode2Net iface =  s.getInterfaces();
			
			//List<String> services = node.getManagmentServices();
			//System.out.println("Get services:  " + services);
		
			//System.out.println("Get ip:  " + node.getManagementIP());
			
			return node.getManagementIP();
		} catch  (Exception e){
			e.printStackTrace();
			System.err.println("Proxy factory test failed");
			assert(false);
		}
		return "";
	}
        
        /*
        This method generates a HTCondor pool request with various parameters passed through NewRequestInfo
        */
        public String generateNewCondorRequest(NewRequestInfo newReq, String orcaSliceID){


            System.out.println("generateNewCondorRequest: START");


            String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
            String sshKeyLocation = rmProperties.getProperty(USER_SSHKEY_PATH_PROP);
            String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);

            String sliceName =  orcaSliceID;


            ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);		

            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            try {
                SSHAccessTokenFileFactory fac;
                fac = new SSHAccessTokenFileFactory(sshKeyLocation, false);

                SSHAccessToken t = fac.getPopulatedToken();			
                sctx.addToken("root", "root", t);
                sctx.addToken("root", t);
            } catch (UtilTransportException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }




            Slice s = Slice.create(sliceProxy, sctx, sliceName);


            // Parse what fields are present in newReq and generate appropriate ndl request
            String templateType = newReq.getTemplateType();
            logger.debug("Start generating ndl request for request type : " + templateType);

            // Basic master-worker topology for Condor Pool
            //ComputeNode master     = s.addComputeNode("Master");
            //ComputeNode workers    = s.addComputeNode("Workers");
            //BroadcastNetwork net   = s.addBroadcastLink("Network");

            //Interface masterIface  = net.stitch(master);
            //Interface workersIface = net.stitch(workers);

            int workersNodeGroupSize = 0;
            // Choose number of workers, max nodecount 
            if(newReq.getNewCompRes() <= 0){
                logger.info("Using default number of workers = " + CondorDefaults.getDefaultNumWorkers());
                workersNodeGroupSize = CondorDefaults.getDefaultNumWorkers();
            }
            else{
                logger.info("Using user-supplied number of workers = " + newReq.getNewCompRes());
                workersNodeGroupSize = newReq.getNewCompRes();          
            }

            // Basic master-worker topology for Condor Pool
            BroadcastNetwork net   = s.addBroadcastLink("Network");
            ComputeNode master  = s.addComputeNode("Master");
            Interface masterIface  = net.stitch(master);
            // This will create the required number of workers and plumb them to the broadcast network, net
            NodeGroup workers = new NodeGroup(workersNodeGroupSize, "Workers", s, net);  
                        
            workers.setMaxNodeCount(CondorDefaults.getDefaultMaxNumWorkers());
            master.setNodeCount(1);
            master.setMaxNodeCount(1);
                        
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

            //ArrayList<String> preferredDomains = new ArrayList<String> ();
            //preferredDomains.add("RENCI (Chapel Hill, NC USA) XO Rack");
            //preferredDomains.add("TAMU (College Station, TX, USA) XO Rack");
            //preferredDomains.add("UCD (Davis, CA USA) XO Rack");

            ArrayList<String> preferredDomains = populatePreferredDomains(rmProperties);
            logger.info("Preferred set of domains = " + preferredDomains);

            ArrayList<String> finalDomains = new ArrayList<String>();
            // Get list of all domains from ndllib
            ArrayList<String> allDomains = new ArrayList<String> (Slice.getDomains());
            if(allDomains != null && !allDomains.isEmpty()){
                logger.info("There are " + allDomains.size() + " available domains");
                logger.info("The available domains from ndllibs are " + allDomains);
                if(preferredDomains != null && !preferredDomains.isEmpty()){ // there is a set of preferred domains; Use only those
                    for (String pDomain: preferredDomains){
                        if(allDomains.contains(pDomain)){ // valid preferred domain
                            finalDomains.add(pDomain);
                        }
                    }
                }
                else{ // no preferred domains
                    finalDomains = allDomains; // all domains can be used
                }
            }
            else{ // ndllib didn't return available domains
                logger.info("ndllib didn't return available set of domains");
                if(preferredDomains != null && !preferredDomains.isEmpty()){
                    logger.info("Using preferredDomains in configuration file as final set of domains");
                    finalDomains = preferredDomains; // whatever is specifed through the configuration becomes the finalDomains;l trust preferred domain names
                }
                else{
                    logger.error("No valid domains found");
                    logger.error("No unbound request supported yet.. returning null...");
                    return null;
                }
            }

            //int numDomains = ComputeDomains.values().length;
            //int pick = new Random().nextInt(numDomains);
            //String domainName1 = ComputeDomains.values()[pick].name; // master domain
            //String domainName2 = ComputeDomains.values()[(pick+1)%numDomains].name; 

            logger.info("Final set of domains used = " + finalDomains);

            int numDomains = finalDomains.size();
            int pick = new Random().nextInt(numDomains);
            String domainName1 = finalDomains.get(pick);
            String domainName2 = finalDomains.get((pick+1)%numDomains);

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
                if(templateType.contains(MultiSuffix)){ // 
                    logger.info("Using default multi-point postboot script for master");
                    CondorDefaults cd = new CondorDefaults();
                    master.setPostBootScript(cd.getDefaultPostbootMaster_MultiPoint());
                }
                else{
                    logger.info("Using default single domain postboot script for master");
                    CondorDefaults cd = new CondorDefaults();
                    master.setPostBootScript(cd.getDefaultPostbootMaster_SingleDomain());
                }
            }
            else {
                logger.info("Using user-supplied postboot script for master");
                master.setPostBootScript(newReq.getNewPostbootMaster());
            }

            // Choose postbootscript for workers
            if(newReq.getNewPostbootWorker() == null){ // No postboot script supplied by user; use default postboot script
                if(templateType.contains(MultiSuffix)){
                    logger.info("Using default multi-point postboot script for workers");
                    CondorDefaults cd = new CondorDefaults();
                    workers.setPostBootScript(cd.getDefaultPostbootWorker_MultiPoint());
                }
                else{
                    logger.info("Using default single domain postboot script for workers");
                    CondorDefaults cd = new CondorDefaults();
                    workers.setPostBootScript(cd.getDefaultPostbootWorker_SingleDomain());
                }
            }
            else {
                logger.info("Using user-supplied postboot script for workers");
                workers.setPostBootScript(newReq.getNewPostbootWorker());
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
            if(templateType.contains(StorageSuffix)){ //Storage is requested
                // Create new storage node and attach to master node
                // TODO: Handle storage
                BroadcastNetwork storageNet = s.addBroadcastLink("StorageNetwork");
                StorageNode storage = s.addStorageNode("Storage");

                Interface masterStorageIface  = storageNet.stitch(master);
                Interface storageStorageIface  = storageNet.stitch(storage);

                // Set properties of storage
                logger.info("Setting domain for Storage to " + domainName1);
                storage.setDomain(domainName1); // same as master domain

                if(newReq.getNewStorage() <= 0){
                    logger.info("Using default amount of storage = " + CondorDefaults.getDefaultStorage());
                    storage.setCapacity(CondorDefaults.getDefaultStorage());
                }
                else{
                    logger.info("Using user-supplied amount of storage = " + newReq.getNewStorage());
                    storage.setCapacity(newReq.getNewStorage());
                }

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
                        //TODO: FIX this
                        int label = spInfo.getVlanTagSet().get(0); // get the first available vlan tag
                        String port = spInfo.getPortSet().get(0);

                        StitchPort  data       = s.addStitchPort("Data", Integer.toString(label),port,10000000000l);
                        Interface dataIface    = net.stitch(data);

                        logger.info("Using stitchport vlan tag = " + label);
                        logger.info("Using stitchport port urn = " + port);

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

            //logger.debug("Generated request = " + "\n" + s.getRequest());

            // For debugging saving the request to a file in /tmp
            //s.save("/tmp/generatedRequest.rdf");

            return (s.getRequest());        

        }        
        
        public String generateNewHadoopRequest(NewRequestInfo newReq){
            return null;
        }
    
        public String generateNewMPIRequest(NewRequestInfo newReq){
            return null;
        }
        
        
        public String getSliceManifestStatus(String manifest){
                        
            Slice s = Slice.loadManifest(manifest);
            // TODO: Looks like s.getState() is not implemented
            return s.getState();
            
            
        }
        
        public int getNumWorkersInManifest(String manifest){
        
            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Workers..");
                return -1;
            }
            int numWorkers = 0;
            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                numWorkers++;
            }
            logger.info("There are " + numWorkers + " worker nodes in the current manifest");
            return numWorkers;
        
        }
        
        public int getNumActiveWorkersInManifest(String manifest){

            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Workers..");
                return -1;
            }
            int numActiveWorkers = 0;
            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(mn.getState().equalsIgnoreCase("Active")){
                    numActiveWorkers++;
                }
            }
            logger.info("There are " + numActiveWorkers + " Active worker nodes in the current manifest");
            return numActiveWorkers;

        }        
        
        public int getNumTicketedWorkersInManifest(String manifest){
        
            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Workers..");
                return -1;
            }
            int numTicketedWorkers = 0;
            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(mn.getState().equalsIgnoreCase("Ticketed")){
                    numTicketedWorkers++;
                }
            }
            logger.info("There are " + numTicketedWorkers + " Ticketed worker nodes in the current manifest");
            return numTicketedWorkers;
        
        }    

        public int getNumNascentWorkersInManifest(String manifest){

            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Workers..");
                return -1;
            }
            int numNascentWorkers = 0;
            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(mn.getState().equalsIgnoreCase("Nascent")){
                    numNascentWorkers++;
                }
            }
            logger.info("There are " + numNascentWorkers + " Nascent worker nodes in the current manifest");
            return numNascentWorkers;

        }    
    
        public boolean areAllWorkersInManifestActive(String manifest){

            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Workers");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Workers..");
                return false;
            }

            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(!mn.getState().equalsIgnoreCase("Active")){
                    return false;
                }
            }
            // Code gets here when all are "Active"
            return true;

        }
        
        public String getPublicIPMasterInManifest(String manifest){
        
            Slice s = Slice.loadManifest(manifest);
            ComputeNode cn = (ComputeNode) s.getResourceByName("Master");
            if(cn == null){
                logger.error("Manifest doesn't have a Nodegroup named Master..");
                return null;
            }

            for (Node mn : ((ComputeNode)cn).getManifestNodes()){
                logger.info("manifestNode: " + mn.getURI() + ", state = " + mn.getState());
                if(mn.getState().equalsIgnoreCase("Active")){
                    // returns the public IP of the first Active node in Master nodegroup; since there is only one 
                    // master node in the nodegroup, this is fine
                    return mn.getPublicIP();
                }
            }

            return null;        
        }
        
        
        private void doSSHAndKillCondor(String publicIP) {
        
            String scriptName = rmProperties.getProperty(KILLCONDORONDELETE_SSH_SCRIPTNAME_PROP_NAME);
            if (scriptName == null || scriptName.isEmpty()){
                logger.error("No condor delete script specified in rm.properties.. can't ssh and stop condor on worker..");
                return;
            }

            String pathToSSHPrivKey = rmProperties.getProperty(KILLCONDORONDELETE_SSH_PRIVKEY_PROP_NAME);
            if (pathToSSHPrivKey == null || pathToSSHPrivKey.isEmpty()){
                logger.error("No ssh priv key path specified in rm.properties.. can't ssh and stop condor on worker..");
                return;
            }

            String sshOpts = "-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no";
            String hostName = publicIP;

            String sshUserName = rmProperties.getProperty(KILLCONDORONDELETE_SSH_USER_PROP_NAME);
            if(sshUserName == null || sshUserName.isEmpty()){
                sshUserName = "root";
            }

            logger.info("Using " + sshUserName + " user to ssh to " + publicIP);
            String sshCmd = "ssh" + " " + sshOpts + " " + "-i" + " " + pathToSSHPrivKey + " " + sshUserName + "@" + hostName + " " + scriptName;
            logger.info("sshCmd = \n " + sshCmd);

            try {
                    Runtime rt = Runtime.getRuntime();
                    Process pr = rt.exec(sshCmd);

                    BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

                    String line=null;

                    while((line=input.readLine()) != null) {
                        logger.info(line);
                    }

                    int exitVal = pr.waitFor();
                    logger.info("ssh command exited with code " + exitVal);
                    if(exitVal != 0){
                        logger.error("Problem ssh-ing and running command in " + sshUserName + "@" + hostName);
                        return;
                    }
                    else{
                        logger.info("Successfully ssh-ed and killed condor daemon on " + hostName);
                        return;
                    }

            } catch(Exception e) {
                System.out.println(e.toString());
                e.printStackTrace();
                logger.error("Exception while running ssh command " + e);
                return;
            }
        
        
        
        }
        
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import orca.ahab.libndl.LIBNDL;
import orca.ahab.libndl.Slice;
import orca.ahab.libndl.TestDriver;
import orca.ahab.libndl.extras.PriorityNetwork;
import orca.ahab.libndl.resources.request.BroadcastNetwork;
import orca.ahab.libndl.resources.request.ComputeNode;
import orca.ahab.libndl.resources.request.Interface;
import orca.ahab.libndl.resources.request.InterfaceNode2Net;
import orca.ahab.libndl.resources.request.Network;
import orca.ahab.libndl.resources.request.Node;
import orca.ahab.libtransport.ISliceTransportAPIv1;
import orca.ahab.libtransport.ITransportProxyFactory;
import orca.ahab.libtransport.PEMTransportContext;
import orca.ahab.libtransport.SSHAccessToken;
import orca.ahab.libtransport.SliceAccessContext;
import orca.ahab.libtransport.TransportContext;
import orca.ahab.libtransport.util.ContextTransportException;
import orca.ahab.libtransport.util.SSHAccessTokenFileFactory;
import orca.ahab.libtransport.util.TransportException;
import orca.ahab.libtransport.util.UtilTransportException;
import orca.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;
import orca.ahab.ndllib.transport.OrcaSMXMLRPCProxy;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMConstants;
import static org.renci.requestmanager.RMConstants.PREFERRED_DOMAINS_STRING_NAME;

/**
 *
 * @author anirban
 */
public class AhabManager implements RMConstants{
    
        // This is the class responsible for talking to ahab and generate requests
        protected Logger logger = null;
        private static Properties rmProperties = null; // need this for preferred domains


        public AhabManager(Properties rmProps){ // constructor required for create requests for getting preferred domains from configuration file
            logger = Logger.getLogger(this.getClass());
            this.rmProperties = rmProps;
        }

        public AhabManager(){
            logger = Logger.getLogger(this.getClass());
        }

        
        public String processModifyNetwork(ModifyRequestInfo modReq, String orcaSliceID){
            return null;
        }
        	
        public String processNewSDXCondor(NewRequestInfo newReq, String orcaSliceID){
        
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
		
		PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX");
                
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
			
                        if(i < 0){ // data site condition TODO
                            
                        }
                        else {
                            for (int j = 0; j < 2; j++){
                                    
                                
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
                                    
                                    sdn.addNode(node,site,ip);
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
		
		PriorityNetwork sdn = PriorityNetwork.create(s, "PegasusHTCondorSDX");
                
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

				sdn.addNode(node,site,ip);
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
}

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
import java.net.Inet4Address;
import java.net.InetAddress;
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
import org.renci.ahab.libndl.resources.request.InterfaceNode2Net;
import org.renci.ahab.libndl.resources.request.Network;
import org.renci.ahab.libndl.resources.request.StitchPort;
import org.renci.ahab.libndl.resources.request.StorageNode;
import org.renci.ahab.libndl.util.IP4Assign;
import org.renci.ahab.libndl.util.IP4Subnet;
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
import org.renci.requestmanager.StitchportInfo;
import org.renci.requestmanager.amqp.DisplayPublisher;
import org.renci.requestmanager.orcaxmlrpc.CleanupCondorAndVM;
import org.renci.requestmanager.policy.IModifyPolicy;
import org.renci.requestmanager.policy.SimpleUnitsModifyPolicy;

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
        String pemLocation = rmProperties.getProperty(USER_CERTKEYFILE_PATH_PROP);
        String sshKeyLocation = rmProperties.getProperty(USER_SSHKEY_PATH_PROP);
        String controllerUrl = rmProperties.getProperty(DEFAULT_CONTROLLERURL_PROP);

        String sliceName =  orcaSliceID;
        ISliceTransportAPIv1 sliceProxy = getSliceProxy(pemLocation, controllerUrl);

        //SSH context
        SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
        try {
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory(sshKeyLocation, false);
            SSHAccessToken t = fac.getPopulatedToken();
            sctx.addToken("root", "root", t);
            sctx.addToken("root", t);
        } catch (UtilTransportException e) {
            e.printStackTrace();
        }

        Slice s = Slice.create(sliceProxy, sctx, sliceName);

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
        s.commit(10,10);
        return "SUCCESS";
    }

    public String processNewSDXCondorTest(NewRequestInfo newReq, String orcaSliceID){

        System.out.println("processNewSDXCondor: START");

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
            ComputeNode  node = (ComputeNode) s.getResourceByName(nodeName);
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
        // The nodes in the nodegroup will be named "Workers-0", "Workers-1", and so on
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
                    // NOTE: If we have to deal with multiple subnets and allowed ips in future, we have that info in spInfo.getAllIPInfo()
                    // Insert StitchportInfo into RMState
                    rmState = RMState.getInstance();
                    rmState.addSPToStitchportList(new StitchportInfo(sliceName, stitchPortID, label, port, firstSubnetNetwork, Integer.parseInt(firstSubnetMask)));
                }              
            }
            s.autoIP();
        }
        else{ // No SP requested
            s.autoIP();
        }

        return (s.getRequest());        
    }        

    public String generateNewHadoopRequest(NewRequestInfo newReq, String orcaSliceID){
        return null;
    }

    public String generateNewMPIRequest(NewRequestInfo newReq, String orcaSliceID){
        return null;
    }


    public String generateModifyComputeRequest(ModifyRequestInfo modReq, String currManifest) {

        Slice s = Slice.loadManifest(currManifest);
        // Run policy to determine change in number of workers
        IModifyPolicy modPolicy = new SimpleUnitsModifyPolicy(logger);
        int change = modPolicy.determineChangeInNumWorkers(modReq, currManifest);

        if(change == 0){
            logger.info("Modify policy determined that no new nodes should be added, and no existing node should be deleted");
            return null;
        }        

        if(change < 0){ // Need to delete some workers           
            int numDeleted = 0;
            int numNodesToDelete = (-1)*change;

            // Go through once to delete the "Tickted" ones first
            for (ComputeNode cn : s.getComputeNodes()){
                if(cn.getName().contains("Workers")){
                    //assuming getURL() returns a unique string for the node
                    logger.info("computeNode: " + cn.getURL() + ", state = " + cn.getState()); 
                    if(numDeleted < numNodesToDelete){
                        if(cn.getState().equalsIgnoreCase("Ticketed")){ 
                            logger.info("deleting Ticketed worker node: " + cn.getURL());
                            cn.delete();
                            numDeleted++;
                        }                    
                    }
                }
            }

            logger.info("Number of Ticketed nodes deleted: " + numDeleted);

            int numNodesMarkedForFutureDeletion = 0;

            // Go through one more time to delete the "Active" ones if more need to be deleted
            for (ComputeNode cn : s.getComputeNodes()){
                if(cn.getName().contains("Workers")){
                    logger.info("computeNode: " + cn.getURL() + ", state = " + cn.getState());
                    if(numDeleted < numNodesToDelete){
                        RMState rmState = RMState.getInstance();
                        String nodeURL = cn.getURL();
                        if(cn.getState().equalsIgnoreCase("Active") && !rmState.isInNodesToBeDeletedIDQ(nodeURL)){ // kill condor only if node is active and is not marked to be deleted in future
                            // if killcondorondelete property exists and is true, ssh to worker and kill condor
                            if(rmProperties.getProperty(KILLCONDORONDELETE_PROP_NAME) != null){
                                String killCondor = rmProperties.getProperty(KILLCONDORONDELETE_PROP_NAME);
                                if(killCondor.equalsIgnoreCase("true")){
                                    logger.info("marking for future deletion of Active manifest node: " + cn.getURL());
                                    numNodesMarkedForFutureDeletion++;
                                    try {
                                        String orcaSliceID = modReq.getOrcaSliceId();
                                        logger.info("Starting thread to cleanup condor and delete VM when condor cleanup is complete...");
                                        // TODO: port CleanupCondorAndVM to AHAB
                                        Thread cleanupCondorAndVMThread = new Thread(new CleanupCondorAndVM(rmProperties, orcaSliceID, cn.getManagementIP(), cn.getURL()));
                                        cleanupCondorAndVMThread.start();
                                        logger.info("Started cleanup thread...");
                                    } catch (Exception ex) {
                                        logger.error("Exception while starting cleanup thread " + ex);
                                    }
                                    numDeleted++;
                                }
                                else {
                                    logger.info("deleting Active manifest node: " + cn.getURL());
                                    cn.delete();
                                    numDeleted++;
                                }
                            }
                            else {
                                logger.info("deleting Active manifest node: " + cn.getURL());
                                cn.delete();
                                numDeleted++;
                            }                        
                        } // end-if active and not marked for future deletion                   
                    } // end-if numDeleted < numNodesToDelete
                } // end-if Worker node
            } // end-for all compute nodes in slice

            logger.info("Number of nodes marked for future deletion: " + numNodesMarkedForFutureDeletion);           
            logger.info("Total number of nodes deleted: " + numDeleted);

            // Publish to display exchange, the modify action
            DisplayPublisher dp;
            try {
                dp = new DisplayPublisher(rmProperties);
                dp.publishInfraMessages(modReq.getOrcaSliceId(), "#workers deleted now = " + (numDeleted - numNodesMarkedForFutureDeletion) + " | #workers marked for future deletion = " + numNodesMarkedForFutureDeletion + " for slice: " + modReq.getOrcaSliceId());
            } catch (Exception ex) {
                logger.error("Exception while publishing to display exchange");
            }


        }

        if (change > 0){ // Need to add more workers

            int numWorkers = 0;
            for (ComputeNode cn : s.getComputeNodes()){
                if(cn.getName().contains("Workers")){
                    logger.info("computeNode: " + cn.getURL() + ", state = " + cn.getState());
                    numWorkers++;
                }
            }
            logger.info("There are " + numWorkers + " worker nodes in the current manifest");
            int newNumWorkers = numWorkers + change;
            logger.info("Making new size of Workers nodegroup to " + newNumWorkers + " by adding " + change + " workers");
            //cn.setNodeCount(newNumWorkers);

            addNewWorkersToNodeGroupInSlice(s, currManifest, modReq.getOrcaSliceId(), change);

            // Publish to display exchange, the modify action
            DisplayPublisher dp;
            try {
                dp = new DisplayPublisher(rmProperties);
                dp.publishInfraMessages(modReq.getOrcaSliceId(), "Making new size of Workers nodegroup to " + newNumWorkers + " by adding " + change + " workers " + " for slice: " + modReq.getOrcaSliceId());
            } catch (Exception ex) {
                logger.error("Exception while publishing to display exchange");
            }
        }     
        return s.getRequest();
    }


    /**
     * For testing addNewWorkersToNodeGroupInSlice
     * @param manifest 
     */
    public String addNewWorkersToNodeGroupInSlice(String manifest, String orcaSliceID){
        Slice s = Slice.loadManifest(manifest);
        addNewWorkersToNodeGroupInSlice(s, manifest, orcaSliceID, 1);
        return s.getRequest();
    }

    public void addNewWorkersToNodeGroupInSlice(Slice s, String manifest, String orcaSliceID, int count) {

        BroadcastNetwork net = null;
        for(BroadcastNetwork bn: s.getBroadcastLinks()){
            if(bn.getName().contains("Network")){
                net = bn;
            }
        }

        if(net == null){
            logger.error("Couldn't find broadcast network called 'Network'");
            return;
        }


        // To obtain list of IP addresses and netmasks for existing nodes in slice            
        ArrayList<IPNetmask> existingIPNetMaskListInSlice = null;

        // These will be used to assign IP addresses to the new nodes
        ArrayList<String> newIPList = new ArrayList<String> ();
        String newMask = null;

        // We have to recreate the ipSubnet for both stitchport and no stitchport case
        IP4Subnet ipSubnet = null;
        IP4Assign ipAssign = new IP4Assign();

        if(s.getStitchPorts() == null || s.getStitchPorts().size() <= 0){ // No stitchports

            try {
                ipSubnet = ipAssign.getAvailableSubnet(CondorDefaults.getDefaultMaxNumWorkers()); // 256 is the default max number of workers
                ipSubnet.markIPUsed(ipSubnet.getStartIP());
            } catch (Exception e){
                logger.warn("allocateSubnet warning: " + e);
            }

            if(ipSubnet != null){                    
                // Mark ips in compute nodes in manifest as used
                existingIPNetMaskListInSlice = getDataPlaneIPAddressesOfComputeNodes(manifest);
                for(IPNetmask ip_nmask: existingIPNetMaskListInSlice){
                    ipSubnet.markIPUsed(ip_nmask.getIP());
                }

                int maskLength = ipSubnet.getMaskLength();
                newMask = IP4Subnet.netmaskIntToString(maskLength);

                for (int j = 0; j < count; j++){
                    String newIP = ipSubnet.getFreeIPs(1).getHostAddress();                    
                    logger.info("new IP address = " + newIP + " | new mask = " + newMask);
                    newIPList.add(newIP);
                }

            }
        }
        else{ // there is a stitchport

            // Recreate ipSubnet from manifest compute nodes and stitchport information
            // mark master and worker IPs as used
            // Then get free IP addresses 

            rmState = RMState.getInstance();
            StitchportInfo spInfo = rmState.findSPFromStitchportList(orcaSliceID);

            if(spInfo == null){
                logger.error("Stitchport exists in the slice but can't find sttichport subnet network IP and netmask information from RMState... Not adding new nodes to the slice !!! ...");
                return;
            }

            String subnetIPFromSPInfo = spInfo.getFirstSubnetIP();
            int maskLengthFromSPInfo = spInfo.getFirstSubnetMask();

            //net.setIPSubnet(ip, mask) functionality
            try{
                ipSubnet = ipAssign.getSubnet((Inet4Address)InetAddress.getByName(subnetIPFromSPInfo), maskLengthFromSPInfo);
                ipSubnet.markIPUsed(ipSubnet.getStartIP());
            } catch (Exception e){
                logger.warn("allocateSubnet warning: " + e);
            }

            // net.clearAvailableIPS() functionality
            if(ipSubnet != null) {
                ipSubnet.markAllIPsUsed();
            }

            //TODO: ArrayList<String> availIPSet = spInfo.getAllowedIPSetFirstSubnet();
            ArrayList<String> availIPSet = null;
            logger.info("available IPs: " + availIPSet);
            for(String ipAvail: availIPSet){
                //net.addAvailableIP(ip);
                int ml = 32;
                int cnt = 1<<(32-ml);
                ipSubnet.markIPsFree(ipAvail, cnt);
            }

            if(ipSubnet != null){
                //ipSubnet.markIPUsed("172.16.0.1");
                //ipSubnet.markIPUsed("172.16.0.2");
                //ipSubnet.markIPUsed("172.16.0.3");

                // Mark ips in compute nodes in manifest as used
                existingIPNetMaskListInSlice = getDataPlaneIPAddressesOfComputeNodes(manifest);
                for(IPNetmask ip_nmask: existingIPNetMaskListInSlice){
                    ipSubnet.markIPUsed(ip_nmask.getIP());
                }

                int maskLength = ipSubnet.getMaskLength();
                newMask = IP4Subnet.netmaskIntToString(maskLength);

                for (int j = 0; j < count; j++){
                    String newIP = ipSubnet.getFreeIPs(1).getHostAddress();                    
                    logger.info("new IP address = " + newIP + " | new mask = " + newMask);
                    newIPList.add(newIP);
                }

            }


        }

        // Finally add the new Worker compute nodes and stitch them to the broadcast network
        int highestIndexOfWorkerNode = getHighestIndexOfWorkerNodeInManifest(manifest);
        String workerDomain = getDomainOfWorkerNodeInManifest(manifest);
        CondorDefaults cd = new CondorDefaults();
        String workerPostboot = cd.getDefaultPostbootWorker_SingleDomain();
        for(int i = 0; i < count; i++){
            int indexNewWorkerNode = highestIndexOfWorkerNode + i + 1; // since i starts from 0, we add 1
            ComputeNode n = s.addComputeNode("Workers" + "-" + indexNewWorkerNode);
            Interface int1 = net.stitch(n); // Stitch to "Network" , which is net
            ((InterfaceNode2Net)int1).setIpAddress(newIPList.get(i));
            ((InterfaceNode2Net)int1).setNetmask(newMask);
            n.setImage(CondorDefaults.getDefaultImageUrl(), CondorDefaults.getDefaultImageHash(), CondorDefaults.getDefaultImageName());
            n.setNodeType("XO Large");
            n.setDomain(workerDomain);                
            n.setPostBootScript(workerPostboot);
        }

    }

    private int getHighestIndexOfWorkerNodeInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);
        int highestIndex = 0;

        for(ComputeNode cn : s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                String workerName = cn.getName(); // Workers-1, Workers-2,....
                int currentIndex = Integer.parseInt(workerName.split("-")[1]);
                if(currentIndex > highestIndex){
                    highestIndex = currentIndex;
                }
            } 
        }

        return highestIndex;
    }

    private String getDomainOfWorkerNodeInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        for(ComputeNode cn : s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                return cn.getDomain();
            } 
        }

        return null;
    }


    public ArrayList<IPNetmask> getDataPlaneIPAddressesOfComputeNodes(String manifest){

        Slice s = Slice.loadManifest(manifest);

        ArrayList<IPNetmask> returnIPNetmaskList = new ArrayList<IPNetmask>();

        BroadcastNetwork net = null;
        for(BroadcastNetwork bn: s.getBroadcastLinks()){
            if(bn.getName().contains("Network")){
                logger.info("Found broadcast network called " + bn.getName());
                net = bn;
            }
        }

        if(net == null){
            logger.error("No broadcast network called 'Network' found in slice");
            return null;
        }

        for(ComputeNode cn : s.getComputeNodes()){
            Interface int1 = cn.getInterface(s.getResourceByName("Network"));
            String dataPlaneIPFromManifest = ((InterfaceNode2Net)int1).getIpAddress();
            String netmaskStringFromManifest = ((InterfaceNode2Net)int1).getNetmask();
            //logger.info("cn: " + cn + " | " + ((InterfaceNode2Net)int1).getIpAddress() + ", " + ((InterfaceNode2Net)int1).getNetmask());

            // dataplaneIP from manifest already has netmask in it; for e.g. 172.mm.p.yyy/xx
            String dataPlaneIP = dataPlaneIPFromManifest.split("/")[0]; // note that this will work even when the IP address doesn't have a /
            logger.info("cn: " + cn + " | " + dataPlaneIP + ", " + netmaskStringFromManifest);
            returnIPNetmaskList.add(new IPNetmask(dataPlaneIP, netmaskStringFromManifest));            
        }

        logger.info("total number of interfaces in slice = " + s.getInterfaces().size());
        //logger.info("total number of links in slice = " + s.getLinks().size());

        return returnIPNetmaskList;

    }


    public String getSliceManifestStatus(String manifest){

        Slice s = Slice.loadManifest(manifest);
        // TODO: Looks like s.getState() is not implemented in AHAB and will return "getState unimplimented"
        // This is only used in manifest publishing; Not dritical as of now
        return s.getState();


    }

    public int getNumWorkersInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        int numWorkers = 0;
        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                numWorkers++;                        
            }
        }

        logger.info("There are " + numWorkers + " worker nodes in the current manifest");
        return numWorkers;

    }

    public int getNumActiveWorkersInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        int numActiveWorkers = 0;
        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                if(cn.getState().equalsIgnoreCase("Active")){
                    numActiveWorkers++;
                    //logger.info("public IP for " + cn.getName() + " is " + cn.getManagementIP());
                }
            }
        }

        logger.info("There are " + numActiveWorkers + " Active worker nodes in the current manifest");
        return numActiveWorkers;

    }        

    public int getNumTicketedWorkersInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        int numTicketedWorkers = 0;
        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                if(cn.getState().equalsIgnoreCase("Ticketed")){
                    numTicketedWorkers++;
                }
            }
        }

        logger.info("There are " + numTicketedWorkers + " Ticketed worker nodes in the current manifest");
        return numTicketedWorkers;

    }    

    public int getNumNascentWorkersInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        int numNascentWorkers = 0;
        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                if(cn.getState().equalsIgnoreCase("Nascent")){
                    numNascentWorkers++;
                }
            }
        }

        logger.info("There are " + numNascentWorkers + " Nascent worker nodes in the current manifest");
        return numNascentWorkers;

    }    

    public boolean areAllWorkersInManifestActive(String manifest){

        Slice s = Slice.loadManifest(manifest);

        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Workers")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                if(!cn.getState().equalsIgnoreCase("Active")){
                    return false;
                }
            }
        }

        // Code gets here when all are "Active"
        logger.info("All Workers nodes are in Active state");
        return true;

    }

    public String getPublicIPMasterInManifest(String manifest){

        Slice s = Slice.loadManifest(manifest);

        for(ComputeNode cn: s.getComputeNodes()){
            if(cn.getName().contains("Master")){
                logger.info("computeNode: " + cn.getName() + ", state = " + cn.getState());
                if(cn.getState().equalsIgnoreCase("Active")){
                    return (cn.getManagementIP());
                }
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

    public class IPNetmask{

        String ip;
        String netmask;

        public IPNetmask(String ip, String netmask){
            this.ip = ip;
            this.netmask = netmask;
        }

        public String getIP(){
            return ip;
        }

        public String getNetmask(){
            return netmask;
        }

    }


}

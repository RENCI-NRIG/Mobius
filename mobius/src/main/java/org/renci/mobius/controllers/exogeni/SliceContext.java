package org.renci.mobius.controllers.exogeni;

import com.google.inject.Guice;
import com.google.inject.Injector;
import exoplex.client.exogeni.SdxExogeniClient;
import injection.SingleSdxModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.ndl.NDLGenerator;
import org.renci.ahab.libndl.resources.request.LinkNetwork;
import org.renci.ahab.libndl.resources.request.*;
import org.renci.ahab.libtransport.*;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.SliceNotFoundOrDeadException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.net.URL;
import java.util.*;
import org.apache.log4j.Logger;

/*
 * @brief class representing resources associated with a slice; it represents all resources associated with
 *        a single mobius request
 *
 * @author kthare10
 */
public class SliceContext {
    private static final Logger LOGGER = Logger.getLogger( SliceContext.class.getName() );

    private ComputeRequest lastRequest;
    private String sliceName;
    private boolean sendNotification;
    private NDLGenerator.SliceState state;
    private Date expiry;

    /*
     * @brief constructor
     *
     * @param sliceName - slice name
     *
     */
    public SliceContext(String sliceName) {
        this.sliceName = sliceName;
        this.lastRequest = null;
        sendNotification = false;
        state = NDLGenerator.SliceState.NULL;
        expiry = null;
    }

    /*
     * @brief determine if notification should be triggered
     *
     * @return true if notification should be triggered; false otherwise
     *
     */
    public boolean canTriggerNotification() {
        if(sendNotification &&
                (state == NDLGenerator.SliceState.STABLE_OK || state == NDLGenerator.SliceState.STABLE_ERROR)) {
            return true;
        }
        return false;
    }

    /*
     * @brief set notification sent flag
     */
    public void setSendNotification(boolean value) {
        sendNotification = value;
    }

    /*
     * @brief get expiry time
     *
     * @return expiry time
     *
     */
    public Date getExpiry() { return expiry; }

    /*
     * @brief returns slice name
     *
     * @return slice name
     */
    public String getSliceName() {
        return sliceName;
    }

    /*
     * @brief set expiry time
     *
     * @parm expiry time
     *
     */
    public void setExpiry(String expiry) {
        long timestamp = Long.parseLong(expiry);
        this.expiry = new Date(timestamp);
    }

    /*
     * @brief get slice proxy
     *
     * @param pem - certificate
     * @param controllerUrl - controller Url
     *
     * @return ISliceTransportAPIv1
     */
    private ISliceTransportAPIv1 getSliceProxy(String pem, String controllerUrl){
        LOGGER.debug("getSliceProxy: IN");

        ISliceTransportAPIv1 sliceProxy = null;
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            LOGGER.debug("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            sliceProxy = ifac.getSliceProxy(ctx, new URL(controllerUrl));

        } catch  (Exception e){
            e.printStackTrace();
            LOGGER.error("Proxy factory test failed");
            assert(false);
        }

        LOGGER.debug("getSliceProxy: OUT");
        return sliceProxy;
    }

    /*
     * @brief get slice
     *
     * @return Slice
     *
     * @throws exception in case of error
     */
    private Slice getSlice() throws Exception {
        LOGGER.debug("getSlice: IN");
        ISliceTransportAPIv1 sliceProxy = getSliceProxy(MobiusConfig.getInstance().getDefaultExogeniUserCertKey(),
                MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
        LOGGER.debug("getSlice: OUT");
        return Slice.loadManifestFile(sliceProxy,sliceName);
    }

    /*
     * @brief function to generate JSONObject representing a single server
     *
     * @param n - server provisioned
     *
     * @return JSONObject representing server
     */
    private JSONObject nodeToJson(Node n){
        LOGGER.debug("nodeToJson: IN");
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, n.getName());
        object.put(CloudContext.JsonKeyState, n.getState());
        int count = 1;
        // Set is used to ensure no duplicate ips are added to JSON object
        Set<String> ipSetForJson = new HashSet<String>();
        if (n instanceof ComputeNode) {
            ComputeNode c = ((ComputeNode) n);
            String mgmtIP = c.getManagementIP();
            object.put(CloudContext.JsonKeyPublicIP, mgmtIP);
            for (Interface i : c.getInterfaces()) {
                InterfaceNode2Net iface = (InterfaceNode2Net) i;
                String ip = iface.getIpAddress();
                if (ip != null && ipSetForJson.contains(ip) == false) {
                    object.put(CloudContext.JsonKeyIP + count,ip);
                    count++;
                    ipSetForJson.add(ip);
                }
            }
        }
        LOGGER.debug("nodeToJson: OUT");
        return object;
    }

    /*
     * @brief function to generate JSONObject representing status of all resources associated with this contexts
     *
     * @param hostNameSet - hostname set
     *
     * @return JSONObject representing status of context
     */
    public JSONObject status(Set<String> hostNameSet) throws Exception{
        LOGGER.debug("status: IN");
        JSONObject returnValue = new JSONObject();
        try {

            Slice slice = getSlice();
            slice.getAllResources();
            if (slice != null) {
                LOGGER.debug("Slice state = " + slice.getState());
                if(slice.getState() == NDLGenerator.SliceState.CLOSING_DEAD) {
                    throw new SliceNotFoundOrDeadException("slice dead");
                }
                returnValue.put(CloudContext.JsonKeySlice, sliceName);
                JSONArray array = new JSONArray();
                for (Node n : slice.getNodes()) {
                    if(n != null) {
                        LOGGER.debug("Node=" + n.getName());
                        JSONObject object = nodeToJson(n);
                        if (object != null && !object.isEmpty()) {
                            array.add(object);
                        }
                        String state = n.getState();
                        if (state.compareToIgnoreCase("Active") == 0) {
                            if (n instanceof ComputeNode && !hostNameSet.contains(n.getName())) {
                                hostNameSet.add(n.getName());
                            }
                        }
                    }
                }
                NDLGenerator.SliceState currState = slice.getState();
                LOGGER.debug("Slice state = " + currState);
                if((currState == NDLGenerator.SliceState.STABLE_OK ||
                        currState == NDLGenerator.SliceState.STABLE_ERROR) &&
                        state != NDLGenerator.SliceState.STABLE_ERROR &&
                        state != NDLGenerator.SliceState.STABLE_OK){
                    if(expiry != null) {
                        // renew the lease to match the end slice
                        slice.renew(expiry);
                        sendNotification = true;
                    }
                }
                state = currState;
                returnValue.put(CloudContext.JsonKeyNodes, array);
            }
        }
        catch (SliceNotFoundOrDeadException e) {
            throw e;
        }
        catch (Exception e){
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occured while getting status of slice " + sliceName);
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("status: OUT");
        }
        return returnValue;
    }

    /*
     * @brief function to release all resources associated with this context
     */
    public void stop() {
        LOGGER.debug("stop: IN");

        try {
            Slice slice = getSlice();
            if(slice != null) {
                slice.delete();
                LOGGER.debug("Successfully deleted slice " + sliceName);
            }
        }
        catch (Exception e){
            LOGGER.debug("Exception occured while deleting slice " + sliceName);
        }
        LOGGER.debug("stop: OUT");
    }

    /*
     * @brief function to periodically check status of all resources associated with slice
     *
     * @param hostNameSet - hostname set
     *
     * @return JSONObject representing status of context
     *
     * @throws SliceNotFoundOrDeadException exception in case slice no longer exists
     */
    public JSONObject doPeriodic(Set<String> hostNameSet) throws SliceNotFoundOrDeadException {
        LOGGER.debug("doPeriodic: IN");

        JSONObject object = null;
        try {
            object = status(hostNameSet);
        }
        catch (SliceNotFoundOrDeadException e) {
            LOGGER.debug("doPeriodic: OUT");
            throw e;
        }
        catch (Exception e){
            if(e.getMessage() != null && (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                LOGGER.debug("doPeriodic: OUT");
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occured while performing periodic updates to slice " + sliceName);
        }
        LOGGER.debug("doPeriodic: OUT");
        return object;
    }

    /*
     * @brief function to process compute request
     *
     * @param nameIndex - number representing index to be added to instance name
     * @param flavorList - list of flavors
     * @param request - compute request
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     */
    public Pair<Integer, Integer> processCompute(List<String> flavorList, int nameIndex,
                                                 int spNameIndex, ComputeRequest request) throws Exception {
        LOGGER.debug("processCompute: IN");

        try {
            Slice slice = null;
            String user = MobiusConfig.getInstance().getDefaultExogeniUser();
            String certKey = MobiusConfig.getInstance().getDefaultExogeniUserCertKey();
            String sshKey = MobiusConfig.getInstance().getDefaultExogeniUserSshKey();

            BroadcastNetwork net = null;

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Exogeni,
                        MobiusConfig.getInstance().getDefaultExogeniUser());
                ISliceTransportAPIv1 sliceProxy  = getSliceProxy(certKey, MobiusConfig.getInstance().getDefaultExogeniControllerUrl());

                //SSH context
                SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
                SSHAccessTokenFileFactory facRoot = new SSHAccessTokenFileFactory(sshKey, false);
                SSHAccessToken tRoot = facRoot.getPopulatedToken();
                sctx.addToken("root", "root", tRoot);
                sctx.addToken("root", tRoot);
                SSHAccessTokenFileFactory facUser = new SSHAccessTokenFileFactory(sshKey, true);
                SSHAccessToken tUser = facUser.getPopulatedToken();
                sctx.addToken(user, user, tUser);
                sctx.addToken(user, tUser);

                slice = Slice.create(sliceProxy, sctx, sliceName);
                if(request.getBandwidth() == null) {
                    net = slice.addBroadcastLink(CloudContext.NetworkName);
                }
                else {
                    long bandwidth = 10000000L;
                    bandwidth = Long.parseLong(request.getBandwidth());
                    net = slice.addBroadcastLink(CloudContext.NetworkName, bandwidth);
                }
            }
            else {
                slice = getSlice();
                Collection<BroadcastNetwork> broadcastNetworks = slice.getBroadcastLinks();
                if(broadcastNetworks.isEmpty()) {
                    throw new MobiusException("No broadcast network found");
                }
                for(BroadcastNetwork b : broadcastNetworks) {
                    if(b.getName().compareToIgnoreCase(CloudContext.NetworkName) == 0) {
                        net = b;
                    }
                }
                if(net == null) {
                    throw new MobiusException("No broadcast network found");
                }
            }

            if(slice == null) {
                throw new MobiusException("Slice could not be created or loaded");
            }

            for (String flavor : flavorList) {
                LOGGER.debug("adding node=" + nameIndex);
                ComputeNode c = null;
                if(request.getHostNamePrefix() == null) {
                    c = slice.addComputeNode(CloudContext.NodeName + nameIndex);
                }
                else {
                    c = slice.addComputeNode(request.getHostNamePrefix() + nameIndex);
                }
                ++nameIndex;
                if (request.getImageUrl() != null && request.getImageHash() != null && request.getImageName() != null) {
                    LOGGER.debug("Request imageUrl=" + request.getImageUrl());
                    LOGGER.debug("Request imageName=" + request.getImageName());
                    LOGGER.debug("Request imageHash=" + request.getImageHash());
                    c.setImage(request.getImageUrl(), request.getImageHash(), request.getImageName());
                } else {
                    LOGGER.debug("Default imageUrl=" + MobiusConfig.getInstance().getDefaultExogeniImageUrl());
                    LOGGER.debug("Default imageName=" + MobiusConfig.getInstance().getDefaultExogeniImageName());
                    LOGGER.debug("Default imageHash=" + MobiusConfig.getInstance().getDefaultExogeniImageHash());
                    c.setImage(MobiusConfig.getInstance().getDefaultExogeniImageUrl(),
                            MobiusConfig.getInstance().getDefaultExogeniImageHash(),
                            MobiusConfig.getInstance().getDefaultExogeniImageName());
                }
                LOGGER.debug("flavor=" + flavor);
                c.setNodeType(flavor);
                String[] arrOfStr = request.getSite().split(":");
                if(arrOfStr.length < 2 || arrOfStr.length > 2) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
                }
                LOGGER.debug("Site=" + request.getSite());
                LOGGER.debug("Domain=" + arrOfStr[1]);
                c.setDomain(arrOfStr[1]);
                InterfaceNode2Net interfaceNode2Net = (InterfaceNode2Net)net.stitch(c);
                if(request.isCoallocate() && request.getIpAddress() != null) {
                    LOGGER.debug("Attaching IP address to the broadcast interface interfaceNode2Net=" + interfaceNode2Net);
                    interfaceNode2Net.setIpAddress(request.getIpAddress());
                }
                if(request.getPostBootScript() != null) {
                    c.setPostBootScript(request.getPostBootScript());
                }
                if(request.isCoallocate()) {
                    if(request.getStitchPortUrl() != null && request.getStitchTag() != null) {
                        long bandwidth = 10000000L;
                        if(request.getStitchBandwidth() != null) {
                            bandwidth = Long.parseLong(request.getStitchBandwidth());
                        }

                        String spName = CloudContext.StitchPortName + spNameIndex;

                        StitchPort stitchPort = slice.addStitchPort(spName, request.getStitchTag(), request.getStitchPortUrl(), bandwidth);
                        InterfaceNode2Net interfaceNode2NetStitch = (InterfaceNode2Net) c.stitch(stitchPort);

                        if(request.getStitchIP() != null) {
                            LOGGER.debug("Attaching IP address to the stitch interface interfaceNode2Net=" + interfaceNode2NetStitch);
                            interfaceNode2NetStitch.setIpAddress(request.getStitchIP());
                        }
                        ++spNameIndex;
                    }
                }
            }

            slice.autoIP();
            sendNotification = true;
            slice.commit(MobiusConfig.getInstance().getDefaultExogeniCommitRetryCount(),
                    MobiusConfig.getInstance().getDefaultExogeniCommitSleepInterval());
            lastRequest = request;
            if(lastRequest.getLeaseEnd() != null) {
                long timestamp = Long.parseLong(lastRequest.getLeaseEnd());
                expiry = new Date(timestamp * 1000);
            }
            // Expiry in 24 hours
            else {
                expiry = new Date();
                expiry.setTime(expiry.getTime() + 604800);
            }
            return  Pair.of(nameIndex, spNameIndex);
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") ||
                            e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("processCompute: OUT");
        }
    }

    /*
     * @brief function to process storge request
     *
     * @param request - storge request
     * @param nameIndex - number representing index to be added to instance name
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     */
    public int processStorageRequest(StorageRequest request, int nameIndex) throws Exception {
        LOGGER.debug("processStorageRequest: IN");

        try {
            Slice slice = getSlice();
            if (slice == null) {
                throw new MobiusException("Unable to load slice");
            }
            ComputeNode c = (ComputeNode) slice.getResourceByName(request.getTarget());
            Collection<Interface> computeInterfaces  = c.getInterfaces();
            if (c == null) {
                throw new MobiusException("Unable to load compute node");
            }
            if(request == null || request.getAction() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Action not specified or invalid");
            }

            boolean commit = false;
            switch (request.getAction()) {
                case ADD: {
                    String storageName = request.getTarget() + CloudContext.StorageNameSuffix + nameIndex;
                    StorageNode storage = (StorageNode) slice.getResourceByName(storageName);
                    if (storage != null) {
                        throw new MobiusException(HttpStatus.BAD_REQUEST, "Storage already exists");
                    }
                    LinkNetwork storageNetwork = slice.addLinkNetwork(request.getTarget() + CloudContext.StorageNetworkName + nameIndex);
                    if (storageNetwork == null) {
                        throw new MobiusException("Unable to load link network node");
                    }
                    LOGGER.debug("Adding storage node = " + storageName);
                    storage = slice.addStorageNode(storageName, request.getSize(), request.getMountPoint());
                    storage.setDomain(c.getDomain());
                    storageNetwork.stitch(storage);
                    storageNetwork.stitch(c, storage);
                    commit = true;
                    sendNotification = true;
                }
                    break;
                case DELETE: {
                    Collection<StorageNode> storageNodes = slice.getStorageNodes();
                    List<StorageNode> storageNodesToBeDeleted = new LinkedList<>();
                    if (storageNodes == null) {
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }

                    for(StorageNode s: storageNodes) {
                        if(s.isAttachedTo(c)) {
                            LOGGER.debug("Added storage=" + s.getName() + " to tobedeleted list");
                            storageNodesToBeDeleted.add(s);
                        }
                    }
                    if(storageNodesToBeDeleted.isEmpty()){
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }
                    for(StorageNode s: storageNodesToBeDeleted) {
                        s.delete();
                    }
                    sendNotification = true;
                    commit = true;
                }
                    break;
                case RENEW: {
                    if(request.getLeaseEnd() == null) {
                        throw new MobiusException(HttpStatus.BAD_REQUEST, "No lease end specified for renew");
                    }
                    Collection<StorageNode> storageNodes = slice.getStorageNodes();
                    List<StorageNode> storageNodesToBeRenewed = new LinkedList<>();
                    if (storageNodes == null) {
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }

                    for(StorageNode s: storageNodes) {
                        if(s.isAttachedTo(c)) {
                            LOGGER.debug("Added storage=" + s.getName() + " to toberenewed list");
                            storageNodesToBeRenewed.add(s);
                        }
                    }
                    if(storageNodesToBeRenewed.isEmpty()){
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }
                    long timestamp = Long.parseLong(request.getLeaseEnd());
                    expiry = new Date(timestamp * 1000);
                    slice.renew(expiry);
                }
                    break;
                default:
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid action on storage");
            }
            if(commit) {
                slice.commit();
            }
            if(request.getAction() == StorageRequest.ActionEnum.ADD) {
                nameIndex++;
            }
            return nameIndex;
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server storage request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("processStorageRequest: OUT");
        }
    }

    /*
     * @brief function to process a stitch request;
     *
     * @param request - stitch request
     * @param nameIndex - number representing index to be added to instance name
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     *
     */
    public int processStitchRequest(StitchRequest request, int nameIndex) throws Exception {
        LOGGER.debug("processStitchRequest: IN");

        try {
            Slice slice = getSlice();
            if (slice == null) {
                throw new MobiusException("Unable to load slice");
            }

            ComputeNode c = (ComputeNode) slice.getResourceByName(request.getTarget());
            if (c == null) {
                throw new MobiusException("Unable to load compute node");
            }

            String spName = CloudContext.StitchPortName + nameIndex;

            long bandwidth = 10000000L;
            if(request.getBandwidth() != null) {
                bandwidth = Long.parseLong(request.getBandwidth());
            }
            StitchPort stitchPort = slice.addStitchPort(spName, request.getTag(), request.getPortUrl(), bandwidth);
            InterfaceNode2Net interfaceNode2Net = (InterfaceNode2Net) c.stitch(stitchPort);

            if(request.getStitchIP() != null) {
                LOGGER.debug("Attaching IP address to the stitch interface interfaceNode2Net=" + interfaceNode2Net);
                interfaceNode2Net.setIpAddress(request.getStitchIP());
            }
            slice.autoIP();
            slice.commit();

            nameIndex++;
            return nameIndex;
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server stitch request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("processStitchRequest: OUT");
        }
    }
    private void stitchToSdx(JSONObject config, String opParams, String routeParams) throws Exception {
        //String sliceName, String IPPrefix, String config, SdxExogeniClient.Operation operation, String operationParams
        Injector injector = Guice.createInjector(new SingleSdxModule());
        SdxExogeniClient sdxExogeniClient = injector.getInstance(SdxExogeniClient.class);
        sdxExogeniClient.readConfigFromJson(config.toString());
        sdxExogeniClient.safeEnabled = false;
        sdxExogeniClient.runExec(opParams);
        if(routeParams != null) {
            System.out.println("routeParams=" + routeParams);
            sdxExogeniClient.runExec(routeParams);
        }
    }
    /*
     * @brief function to process network request
     *
     * @param hostName - hostName
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequestSetupStitchingAndRoute(String hostname, String ip, String subnet, NetworkRequest.ActionEnum action) throws Exception{
        LOGGER.debug("processNetworkRequestSetupStitchingAndRoute: IN");

        try {
            Slice slice = getSlice();
            if (slice == null) {
                throw new MobiusException("Unable to load slice");
            }

            ComputeNode c = (ComputeNode) slice.getResourceByName(hostname);
            if (c == null) {
                throw new MobiusException("Unable to load compute node");
            }

            JSONObject object =  new JSONObject();
            object.put("config.type", "client");

            // hack needed to remove .pub from filename as SDX code expects to not have file extension
            String sshfile = MobiusConfig.getInstance().getDefaultExogeniUserSshKey();
            int indexOfLast = sshfile.lastIndexOf(".pub");
            String newString = null;
            if(indexOfLast >= 0) newString = sshfile.substring(0, indexOfLast);

            object.put("config.sshkey", newString);
            object.put("config.safe", "false");
            object.put("config.exogenipem", MobiusConfig.getInstance().getDefaultExogeniUserCertKey());
            object.put("config.exogenism", MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
            object.put("config.serverurl","http://18.191.204.20:8888/");

            // Stitch Source Node
            object.put("config.slicename", sliceName);
            System.out.println("processNetworkRequestSetupStitchingAndRoute(): source = " + object.toString());
            // TODO: determine IP Address
            String opParams = "stitch ";
            String routeParams = "route ";
            if(action == NetworkRequest.ActionEnum.DELETE) {
                opParams = "unstitch ";
                routeParams = null;
            }
            opParams = opParams + hostname + " " + ip + " " + subnet;
            if(routeParams != null) {
                routeParams = routeParams + subnet + " " + ip;
            }
            System.out.println("processNetworkRequestSetupStitchingAndRoute(): opParams = " + opParams);
            stitchToSdx(object, opParams, routeParams);
            System.out.println("processNetworkRequestSetupStitchingAndRoute(): completed stitchToSdx");

        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server stitch request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("processNetworkRequestSetupStitchingAndRoute: OUT");
        }
    }


    /*
     * @brief function to process network request
     *
     * @param hostName - hostName
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequestLink(String subnet1, String subnet2) throws Exception{
        LOGGER.debug("processNetworkRequestLink: IN");

        try {
            Slice slice = getSlice();
            if (slice == null) {
                throw new MobiusException("Unable to load slice");
            }

            JSONObject object =  new JSONObject();
            object.put("config.type", "client");

            // hack needed to remove .pub from filename as SDX code expects to not have file extension
            String sshfile = MobiusConfig.getInstance().getDefaultExogeniUserSshKey();
            int indexOfLast = sshfile.lastIndexOf(".pub");
            String newString = null;
            if(indexOfLast >= 0) newString = sshfile.substring(0, indexOfLast);

            object.put("config.sshkey", newString);
            object.put("config.safe", "false");
            object.put("config.exogenipem", MobiusConfig.getInstance().getDefaultExogeniUserCertKey());
            object.put("config.exogenism", MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
            object.put("config.serverurl","http://18.191.204.20:8888/");

            // Stitch Source Node
            object.put("config.slicename", sliceName);
            System.out.println("processNetworkRequestLink(): source = " + object.toString());
            String opParams = "link " + subnet1 + " " + subnet2;
            System.out.println("processNetworkRequestLink(): opParams = " + opParams);
            stitchToSdx(object, opParams, null);
            System.out.println("processNetworkRequestLink(): completed stitchToSdx");

        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage() != null &&
                    (e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed"))) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server stitch request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("processNetworkRequestLink: OUT");
        }
    }
}

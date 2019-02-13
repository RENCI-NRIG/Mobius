package org.renci.mobius.controllers.exogeni;

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
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.net.URL;
import java.util.*;
import org.apache.log4j.Logger;

public class SliceContext {
    private static final Logger LOGGER = Logger.getLogger( SliceContext.class.getName() );

    private ComputeRequest lastRequest;
    private String sliceName;
    private boolean sendNotification;
    private NDLGenerator.SliceState state;
    private Date expiry;

    public SliceContext(String sliceName) {
        this.sliceName = sliceName;
        this.lastRequest = null;
        sendNotification = false;
        state = NDLGenerator.SliceState.NULL;
        expiry = null;
    }
    public boolean canTriggerNotification() {
        if(sendNotification &&
                (state == NDLGenerator.SliceState.STABLE_OK || state == NDLGenerator.SliceState.STABLE_ERROR)) {
            return true;
        }
        return false;
    }

    public void setSendNotification(boolean value) {
        sendNotification = value;
    }
    public Date getExpiry() { return expiry; }
    public String getSliceName() {
        return sliceName;
    }

    public void setExpiry(String expiry) {
        long timestamp = Long.parseLong(expiry);
        this.expiry = new Date(timestamp);
    }

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

    private Slice getSlice() throws Exception {
        LOGGER.debug("getSlice: IN");
        ISliceTransportAPIv1 sliceProxy = getSliceProxy(MobiusConfig.getInstance().getDefaultExogeniUserCertKey(),
                MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
        LOGGER.debug("getSlice: OUT");
        return Slice.loadManifestFile(sliceProxy,sliceName);
    }

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
                int nodeCount = slice.getNodes().size();
                for (Node n : slice.getNodes()) {
                    LOGGER.debug("Node=" + n.getName());
                    JSONObject object = nodeToJson(n);
                    if(object != null && !object.isEmpty()) {
                        array.add(object);
                    }
                    String state = n.getState();
                    if (state.compareToIgnoreCase("Active") == 0) {
                        if(n instanceof ComputeNode && !hostNameSet.contains(n.getName())) {
                            hostNameSet.add(n.getName());
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
            if(e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed")) {
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
            if(e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed")) {
                LOGGER.debug("doPeriodic: OUT");
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occured while performing periodic updates to slice " + sliceName);
        }
        LOGGER.debug("doPeriodic: OUT");
        return object;
    }
    public int processCompute(List<String> flavorList, int nameIndex, ComputeRequest request) throws Exception {
        LOGGER.debug("processCompute: IN");

        try {
            Slice slice = null;
            String user = MobiusConfig.getInstance().getDefaultExogeniUser();
            String certKey = MobiusConfig.getInstance().getDefaultExogeniUserCertKey();
            String sshKey = MobiusConfig.getInstance().getDefaultExogeniUserSshKey();

            BroadcastNetwork net = null;

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Exogeni);
                ISliceTransportAPIv1 sliceProxy  = getSliceProxy(certKey, MobiusConfig.getInstance().getDefaultExogeniControllerUrl());

                //SSH context
                SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
                SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory(sshKey, false);
                SSHAccessToken t = fac.getPopulatedToken();
                sctx.addToken("root", "root", t);
                sctx.addToken("root", t);
                sctx.addToken(user, user, t);
                sctx.addToken(user, t);

                slice = Slice.create(sliceProxy, sctx, sliceName);
                net = slice.addBroadcastLink(CloudContext.NetworkName);
            }
            else {
                slice = getSlice();
                if(slice.getBroadcastLinks().isEmpty() || slice.getBroadcastLinks().size() > 1) {
                    throw new MobiusException("Invalid number of broadcast network");
                }
                net = slice.getBroadcastLinks().iterator().next();
            }

            if(slice == null) {
                throw new MobiusException("Slice could not be created or loaded");
            }

            for (String flavor : flavorList) {
                LOGGER.debug("adding node=" + nameIndex);
                ComputeNode c = slice.addComputeNode(CloudContext.NodeName + nameIndex);
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
                net.stitch(c);
                if(request.getPostBootScript() != null) {
                    c.setPostBootScript(request.getPostBootScript());
                }
            }

            slice.autoIP();
            sendNotification = true;
            slice.commit(MobiusConfig.getInstance().getDefaultExogeniCommitRetryCount(),
                    MobiusConfig.getInstance().getDefaultExogeniCommitSleepInterval());
            lastRequest = request;
            long timestamp = Long.parseLong(lastRequest.getLeaseEnd());
            expiry = new Date(timestamp * 1000);
            return nameIndex;
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            if(e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed")) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request");
        }
        finally {
            LOGGER.debug("processCompute: OUT");
        }
    }
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
                    Collection<StorageNode> storageNodes = slice.getStorageNodes();
                    List<StorageNode> storageNodesToBeRenewed = new LinkedList<>();
                    if (storageNodes == null) {
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }

                    for(StorageNode s: storageNodes) {
                        if(s.isAttachedTo(c)) {
                            LOGGER.debug("Added storage=" + s.getName() + " to tobedeleted list");
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
            if(e.getMessage().contains("unable to find slice") || e.getMessage().contains("slice already closed")) {
                // Slice not found
                throw new SliceNotFoundOrDeadException("slice no longer exists");
            }
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request");
        }
        finally {
            LOGGER.debug("processStorageRequest: OUT");
        }
    }
}

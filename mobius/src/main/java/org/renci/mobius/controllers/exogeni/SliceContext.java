package org.renci.mobius.controllers.exogeni;

import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.*;
import org.renci.ahab.libtransport.*;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.net.URL;
import java.util.*;

public class SliceContext {

    enum State {
        New,
        Ticketed,
        Active,
        Closed,
        PartiallyActive
    }
    public static final String JsonKeySlice = "slice";
    public static final String JsonKeyNodes = "nodes";
    public static final String JsonKeyName = "name";
    public static final String JsonKeyState = "state";
    public static final String JsonKeyPublicIP = "publicIP";
    public static final String JsonKeyIP = "ip";

    private ComputeRequest request;
    private String sliceName;
    private boolean sliceRenewed;
    private State state;

    public SliceContext(String sliceName, ComputeRequest request) {
        this.sliceName = sliceName;
        this.request = request;
        sliceRenewed = false;
        state = State.New;
    }
    public String getSliceName() {
        return sliceName;
    }
    public ComputeRequest getRequest() {
        return request;
    }

    private ISliceTransportAPIv1 getSliceProxy(String pem, String controllerUrl){

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

    private Slice getSlice() throws Exception {
        ISliceTransportAPIv1 sliceProxy = getSliceProxy(MobiusConfig.getInstance().getDefaultExogeniUserCertKey(),
                MobiusConfig.getInstance().getDefaultExogeniControllerUrl());
        return Slice.loadManifestFile(sliceProxy,sliceName);
    }

    private JSONObject nodeToJson(Node n){
        JSONObject object = new JSONObject();
        object.put(JsonKeyName, n.getName());
        object.put(JsonKeyState, n.getState());
        int count = 1;
        // Set is used to ensure no duplicate ips are added to JSON object
        Set<String> ipSetForJson = new HashSet<String>();
        if (n instanceof ComputeNode) {
            ComputeNode c = ((ComputeNode) n);
            String mgmtIP = c.getManagementIP();
            object.put(JsonKeyPublicIP, mgmtIP);
            for (Interface i : c.getInterfaces()) {
                InterfaceNode2Net iface = (InterfaceNode2Net) i;
                String ip = iface.getIpAddress();
                if (ip != null && ipSetForJson.contains(ip) == false) {
                    object.put(JsonKeyIP + count,ip);
                    count++;
                    ipSetForJson.add(ip);
                }
            }
        }
        return object;
    }

    public JSONObject status(Set<String> hostNameSet) {
        JSONObject returnValue = new JSONObject();
        try {

            Slice slice = getSlice();
            if (slice != null) {
                returnValue.put(JsonKeySlice, sliceName);
                JSONArray array = new JSONArray();
                int nodeCount = slice.getNodes().size();
                int activeCount = 0;
                int closeCount = 0;
                int ticketedCount = 0;
                for (Node n : slice.getNodes()) {
                    JSONObject object = nodeToJson(n);
                    if(object != null && !object.isEmpty()) {
                        array.add(object);
                    }
                    String state = n.getState();
                    if (state.compareToIgnoreCase("Active") == 0) {
                        if(n instanceof ComputeNode && !hostNameSet.contains(n.getName())) {
                            hostNameSet.add(n.getName());
                        }
                        activeCount++;
                    }
                    if (state.compareToIgnoreCase("Closed") == 0) {
                        closeCount++;
                    }
                    if (state.compareToIgnoreCase("Ticketed") == 0) {
                        ticketedCount++;
                    }
                }

                if(state != State.Active) {
                    if (activeCount == nodeCount) {
                        state = State.Active;
                    }
                    else if (closeCount == nodeCount) {
                        state = State.Closed;
                    }
                    else if (ticketedCount == nodeCount) {
                        state = State.Ticketed;
                    }
                    else if (activeCount + closeCount == nodeCount){
                        state = State.PartiallyActive;
                    }
                }
                if(state == State.Active && !sliceRenewed) {
                    // renew the lease to match the end slice
                    long timestamp = Long.parseLong(request.getLeaseEnd());
                    Date expiry = new Date(timestamp * 1000);
                    slice.renew(expiry);
                    sliceRenewed = true;
                }
                returnValue.put(JsonKeyNodes, array);
            }
        }
        catch (Exception e){
            System.out.println("Exception occured while getting status of slice " + sliceName);
        }
        return returnValue;
    }
    public void stop() {
        try {
            Slice slice = getSlice();
            if(slice != null) {
                slice.delete();
                System.out.println("Successfully deleted slice " + sliceName);
            }
        }
        catch (Exception e){
            System.out.println("Exception occured while deleting slice " + sliceName);
        }
    }
    public Pair<Boolean, JSONObject> doPeriodic(Set<String> hostNameSet) {
        JSONObject object = null;
        boolean sendNotification = sliceRenewed;
        try {
            object = status(hostNameSet);
        }
        catch (Exception e){
            System.out.println("Exception occured while performing periodic updates to slice " + sliceName);
        }
        // Slice was renewed in this periodic cycle; send notification to Pegasus
        if(!sendNotification && sliceRenewed) {
            System.out.println("Slice was renewed in this periodic cycle; send notification to Pegasus");
            sendNotification = true;
        }
        else {
            System.out.println("Notification to Pegasus already sent");
            sendNotification = false;
        }
        return new Pair<>(sendNotification, object);
    }
    public int processCompute(List<String> flavorList, int nameIndex) throws Exception {
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
                System.out.println("adding node=" + nameIndex);
                ComputeNode c = slice.addComputeNode(CloudContext.NodeName + nameIndex);
                ++nameIndex;
                if (request.getImageUrl() != null && request.getImageHash() != null && request.getImageName() != null) {
                    System.out.println("imageUrl=" + request.getImageUrl());
                    System.out.println("imageName=" + request.getImageName());
                    System.out.println("imageHash=" + request.getImageHash());
                    c.setImage(request.getImageUrl(), request.getImageHash(), request.getImageName());
                } else {
                    System.out.println("imageUrl=" + MobiusConfig.getInstance().getDefaultExogeniImageUrl());
                    System.out.println("imageName=" + MobiusConfig.getInstance().getDefaultExogeniImageName());
                    System.out.println("imageHash=" + MobiusConfig.getInstance().getDefaultExogeniImageHash());
                    c.setImage(MobiusConfig.getInstance().getDefaultExogeniImageUrl(),
                            MobiusConfig.getInstance().getDefaultExogeniImageHash(),
                            MobiusConfig.getInstance().getDefaultExogeniImageName());
                }
                System.out.println("flavor=" + flavor);
                c.setNodeType(flavor);
                String[] arrOfStr = request.getSite().split(":");
                if(arrOfStr.length < 2 || arrOfStr.length > 2) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
                }
                System.out.println("Site=" + request.getSite());
                System.out.println("Domain=" + arrOfStr[1]);
                c.setDomain(arrOfStr[1]);
                net.stitch(c);
            }

            slice.autoIP();

            slice.commit(MobiusConfig.getInstance().getDefaultExogeniCommitRetryCount(),
                    MobiusConfig.getInstance().getDefaultExogeniCommitSleepInterval());

            return nameIndex;
        }
        catch (MobiusException e) {
            System.out.println("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            System.out.println("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request");
        }
    }
    public void processStorageRequest(StorageRequest request) throws Exception {
        try {
            Slice slice = getSlice();
            if (slice == null) {
                throw new MobiusException("Unable to load slice");
            }
            ComputeNode c = (ComputeNode) slice.getResourceByName(request.getTarget());
            if (c == null) {
                throw new MobiusException("Unable to load compute node");
            }
            System.out.println("Domain=" + c.getDomain());
            BroadcastNetwork storageNetwork = (BroadcastNetwork) slice.getResourceByName(CloudContext.StorageNetworkName);
            if (storageNetwork == null) {
                storageNetwork = slice.addBroadcastLink(CloudContext.StorageNetworkName);
            }
            String storageName = request.getTarget() + CloudContext.StorageNameSuffix;

            StorageNode storage = (StorageNode) slice.getResourceByName(storageName);
            switch (request.getAction()) {
                case ADD:
                    if (storage != null) {
                        throw new MobiusException(HttpStatus.BAD_REQUEST, "Storage already exists");
                    }
                    System.out.println("Adding storage node = " + storageName);
                    storage = slice.addStorageNode(storageName);
                    storage.setDomain(c.getDomain());
                    storage.setCapacity(request.getSize());
                    // TODO mountpoint
                    storageNetwork.stitch(storage);
                    storageNetwork.stitch(c);
                    break;
                case DELETE:
                    if (storage == null) {
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }
                    storage.delete();
                    break;
                case UPDATE:
                    if (storage == null) {
                        throw new MobiusException(HttpStatus.NOT_FOUND, "Storage does not exist");
                    }
                    storage.setCapacity(request.getSize());
                    break;
                default:
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid action on storage");
            }
            slice.commit();
        }
        catch (MobiusException e) {
            System.out.println("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            System.out.println("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request");
        }
    }
}

package org.renci.mobius.controllers.chameleon;

import com.google.common.collect.ImmutableSet;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.NetworkController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.ComputeResponse;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.*;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

/*
 * @brief class represents context for all resources on a specific region on chameleon. It maintains
 *        StackContext per request received.
 *
 * @author kthare10
 */
public class ChameleonContext extends CloudContext implements AutoCloseable {
    private static final String stitchablePhysicalNetwork = "exogeni";
    private static final Logger LOGGER = LogManager.getLogger( ChameleonContext.class.getName() );
    private static final Long maxDiffInSeconds = 604800L;
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final static TimeZone utc = TimeZone.getTimeZone("UTC");
    public final static String NetworkLeaseId = "networkLeaseId";


    private HashMap<String, StackContext> stackContextHashMap;
    private Map<String, String> workflowNetwork = null;
    private Map<String, String> metaData = null;
    private String region = null;
    private  NetworkController networkController = null;
    private OsReservationApi api = null;
    /*
     * @brief constructor
     *
     * @param t - cloud type
     * @param s - site
     * @param workflowId - workflow id
     *
     * @throws Exception in case region could not be determined from site
     *
     */
    public ChameleonContext(CloudContext.CloudType t, String s, String workflowId) throws Exception {
        super(t, s, workflowId);
        stackContextHashMap = new HashMap<>();
        String[] arrOfStr = s.split(":");
        if (arrOfStr.length < 2 || arrOfStr.length > 2) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
        }
        region = arrOfStr[1];

        if(MobiusConfig.getInstance().getCometHost() != null) {
            metaData = new HashMap<>();
            metaData.put("cometpubkeysgroupwrite", "all");
            metaData.put("cometpubkeysgroupread", "all");
            metaData.put("comethostsgroupread", "all");
            metaData.put("comethostsgroupwrite", "all");
            metaData.put("comethost", MobiusConfig.getInstance().getCometHost());
            metaData.put("slicecometwritetoken", workflowId + "write");
            metaData.put("slicecometreadtoken", workflowId + "read");
            metaData.put("slice_id", workflowId);
            metaData.put("reservation_id", "");
        }

        String user = MobiusConfig.getInstance().getChameleonUser();
        String password = MobiusConfig.getInstance().getChameleonUserPassword();
        String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
        String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
        String project = MobiusConfig.getInstance().getChameleonProject();
        String projectDomain = MobiusConfig.getInstance().getJetStreamProjectDomain();

        networkController = new NetworkController(authurl, user, password, userDomain, project);
        api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);
    }

    /*
     * @brief close networkcontroller
     */
    public void close() {
        networkController.close();
    }

    /*
     * @brief setup private network for chameleon region; all instances are connected to each other via this
     *        network;
     */
    private String setupNetwork(ComputeRequest request) throws Exception {
        LOGGER.debug("IN: request=" + request.toString());
        LOGGER.debug("workflowNetwork=" + workflowNetwork);
        String networkId = null;
        try {

            if (workflowNetwork != null && workflowNetwork.containsKey(NetworkController.NetworkId)) {
                LOGGER.debug("Workflow network already exists - returning workflow network id");
                return workflowNetwork.get(NetworkController.NetworkId);
            }

            // If network type is default; use chameleon default network i.e. sharednet1
            if (request.getNetworkType() == ComputeRequest.NetworkTypeEnum.DEFAULT) {
                return networkController.getNetworkId(region, MobiusConfig.getInstance().getChameleonDefaultNetwork());
            }

            String externalNetworkId = networkController.getNetworkId(region, request.getExternalNetwork());
            String networkName = workflowId + CloudContext.generateRandomString();
            LOGGER.debug("Setting up Network for " + region + " network=" + networkName);

            List<String> dnsServers = null;
            if(region.compareToIgnoreCase(StackContext.RegionUC) == 0) {
                dnsServers= MobiusConfig.getInstance().getUcChameleonDnsServers();
            }
            else {
                dnsServers= MobiusConfig.getInstance().getTaccChameleonDnsServers();
            }
            String gatewayIp = request.getNetworkCidr();
            gatewayIp = gatewayIp.substring(0, gatewayIp.lastIndexOf(".") + 1) + "254";

            if(request.getPhysicalNetwork().compareToIgnoreCase(stitchablePhysicalNetwork) == 0) {
                sdf.setTimeZone(utc);
                Date endTime = new Date();
                if(request.getLeaseEnd() != null) {
                    endTime = new Date(Long.parseLong(request.getLeaseEnd()) * 1000);
                }
                else {
                    endTime.setTime(endTime.getTime() + 86400000);
                }

                Date now = new Date();
                now.setTime(now.getTime() + 60000);

                String leaseName = networkName + "lease";
                String reservationRequest = api.buildNetworkLeaseRequest(leaseName, networkName,
                        request.getPhysicalNetwork(), sdf.format(now), sdf.format(endTime));

                if(reservationRequest == null) {
                    throw new MobiusException("Failed to construct reservation request");
                }

                Pair<String, Map<String, Integer>> result = api.createLease(region, leaseName,
                        reservationRequest, 300);

                if(result == null || result.getFirst() == null || result.getSecond() == null) {
                    throw new MobiusException("Failed to request lease");
                }

                String leaseId = result.getFirst();
                workflowNetwork = networkController.updateNetwork(region, networkName, externalNetworkId,
                        request.getNetworkCidr(), gatewayIp, dnsServers);
                workflowNetwork.put(NetworkLeaseId, leaseId);
            }
            else {
                // Workflow network for region does not exist create workflow private network
                workflowNetwork = networkController.createNetwork(region, request.getPhysicalNetwork(),
                        externalNetworkId, false, request.getNetworkCidr(), gatewayIp, dnsServers, networkName, false);
            }

            networkId = workflowNetwork.get(NetworkController.NetworkId);
        }
        catch (Exception e){
            LOGGER.error("Exception occured while setting up network ");
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
            throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to setup network " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("OUT: networkId=" + networkId);
        }
        return networkId;
    }

    /*
     * @brief validate compute request; ignore leaseStart and leaseEnd time validation for future requests
     *
     * @param request - compute request
     * @param isFutureRequest - flag indicating if request is future request
     *
     * @throws Exception in case validation fails
     *
     */
    protected void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("IN: request="+ request.toString() + " isFutureRequest=" + isFutureRequest);

        if(request.getCpus() == 0 && request.getGpus() == 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "No cpus and gpus are requested");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.PRIVATE) {
            if(request.getPhysicalNetwork() == null || request.getExternalNetwork() == null ||
                    request.getNetworkCidr() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST,
                        "Required externalNetwork, physicalNetwork or networkCidr not specified for private network");
            }
        }
        else {
            if(request.getIpAddress() != null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "IP address allocation not allowed for default network");
            }
        }

        if(request.getHostNamePrefix() != null && !request.getHostNamePrefix().matches("[a-zA-Z]+")) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Host Name prefix can only contain alphabet characters");
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, maxDiffInSeconds);
        LOGGER.debug("OUT");
    }


    /*
     * @brief validate storage requests; ignore leaseStart and leaseEnd time validation for future requests
     *
     * @param request - storage request
     * @param isFutureRequest - flag indicating if request is future request
     *
     * @throws Exception in case validation fails
     *
     */
    protected void validateStorageRequest(StorageRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("IN request=" + request.toString() + " isFutureRequest=" + isFutureRequest);
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, maxDiffInSeconds);
        LOGGER.debug("OUT");
    }

    /*
     * @brief function to generate JSONArray representing all the stack contexts held in this context
     *
     * @return JSONArray
     */
    @Override
    public JSONArray toJson() {
        synchronized (this) {
            JSONArray slices = null;
            if (stackContextHashMap != null && stackContextHashMap.size() != 0) {
                slices = new JSONArray();
                StackContext context = null;
                for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                    context = entry.getValue();
                    JSONObject slice = context.toJson();
                    slices.add(slice);
                }
            }
            return slices;
        }
    }

    /*
     * @brief build the context from JSONArray read from database; invoked when contexts are loaded
     *        on mobius restart
     *
     * @param array - json array representing all the stack contexts
     */
    @Override
    public void fromJson(JSONArray array) {
        synchronized (this) {
            if (array != null) {
                for (Object object : array) {
                    JSONObject slice = (JSONObject) object;
                    String sliceName = (String) slice.get("name");
                    LOGGER.debug("sliceName=" + sliceName);
                    StackContext sliceContext = new StackContext(sliceName, workflowId, region);
                    sliceContext.fromJson(slice);
                    stackContextHashMap.put(sliceName, sliceContext);
                }
            } else {
                LOGGER.error("Null array passed");
            }
        }
    }

    /*
     * @brief add cloud specific info to JSON Object representing ChameleonContext;
     *        JSON Object is saved to database
     *
     * @param object - json object representing ChameleonContext
     */
    @Override
    public JSONObject addCloudSpecificDataToJson(JSONObject object) {
        if(workflowNetwork != null) {
            String networkLeaseId = workflowNetwork.get(NetworkLeaseId);
            String networkId = workflowNetwork.get(NetworkController.NetworkId);
            String subnetId = workflowNetwork.get(NetworkController.SubnetId);
            String routerId = workflowNetwork.get(NetworkController.RouterId);
            String networkName = workflowNetwork.get(NetworkName);
            if(networkLeaseId != null) {
                object.put(NetworkLeaseId, networkLeaseId);
            }
            if(networkId != null) {
                object.put(NetworkController.NetworkId, networkId);
            }
            if(subnetId != null) {
                object.put(NetworkController.SubnetId, subnetId);
            }
            if(routerId != null) {
                object.put(NetworkController.RouterId, routerId);
            }
            if(networkName != null) {
                object.put(NetworkName, networkName);
            }
        }
        return object;
    }

    /*
     * @brief function to load cloud specific data from JSON Object representing Chameleon Context
     *
     * @param object - json object representing ChameleonContext
     */
    @Override
    public void loadCloudSpecificDataFromJson(JSONObject object) {
        String networkLeaseId = (String) object.get(NetworkLeaseId);
        String networkId = (String) object.get(NetworkController.NetworkId);
        String subnetId = (String) object.get(NetworkController.SubnetId);
        String routerId = (String) object.get(NetworkController.RouterId);
        String networkName = (String) object.get(NetworkName);
        if(networkId != null) {
            workflowNetwork = new HashMap<>();
            workflowNetwork.put(NetworkController.NetworkId, networkId);
        }
        if(subnetId != null) {
            workflowNetwork.put(NetworkController.SubnetId, subnetId);
        }
        if(routerId != null) {
            workflowNetwork.put(NetworkController.RouterId, routerId);
        }
        if(networkLeaseId != null) {
            workflowNetwork.put(NetworkLeaseId, networkLeaseId);
        }
        if(networkName != null) {
            workflowNetwork.put(NetworkName, networkName);
        }
    }

    /*
     * @brief function to process compute request
     *
     * @param request - compute request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return ComputeResponse
     */
    @Override
    public ComputeResponse processCompute(ComputeRequest request, int nameIndex, int spNameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN request=" + request.toString() + " nameIndex=" + nameIndex + " spNameIndex=" + spNameIndex + " isFutureRequest=" + isFutureRequest);
            validateComputeRequest(request, isFutureRequest);

            int retries = MobiusConfig.getInstance().getChameleonLeaseRetry(), count = 0;
            while(count++ < retries){
                Map<String, Integer> flavorList = ChameleonFlavorAlgo.determineFlavors(request.getCpus(), request.getGpus(),
                        request.getRamPerCpus(), request.getDiskPerCpus(), request.getForceflavor());

                if (flavorList == null) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
                }
                LOGGER.debug("Flavorlist = " + flavorList);

                String sliceName = null;
                StackContext context = new StackContext(sliceName, workflowId, region);

                try {
                    String networkId = setupNetwork(request);
                    // For chameleon; slice per request mechanism is followed
                    ComputeResponse response = context.provisionNode(flavorList, nameIndex, request.getImageName(),
                            request.getLeaseEnd(), request.getHostNamePrefix(), request.getPostBootScript(),
                            metaData, networkId, request.getIpAddress());
                    LOGGER.debug("Created new context=" + sliceName);

                    sliceName = context.getSliceName();
                    stackContextHashMap.put(sliceName, context);
                    LOGGER.debug("Added " + sliceName);
                    response.setStitchCount(spNameIndex);
                    return response;
                } catch (LeaseException e) {
                    // Retry only when flavor is not forced
                    if(count+1 == retries || request.getForceflavor() != null) {
                        throw e;
                    }
                    else {
                        LOGGER.debug("Retry request = " + count);
                        if(context != null) {
                            context.stop();
                        }
                    }
                }
                catch (Exception e) {
                    throw e;
                }
                finally {
                    LOGGER.debug("OUT");
                }
            }
        }
        LOGGER.debug("OUT");
        throw new MobiusException("failed to serve compute request - should never occur");
    }

    /*
     * @brief function to process storge request
     *
     * @param request - storge request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     */
    @Override
    public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN request=" + request + " nameIndex=" + nameIndex + " isFutureRequest=" + isFutureRequest);
            validateStorageRequest(request, isFutureRequest);

            switch (request.getAction()) {
                case ADD: {
                    Map<String, Integer> flavorList = ChameleonFlavorAlgo.determineFlavors(request.getSize());

                    if (flavorList == null) {
                        throw new MobiusException(HttpStatus.BAD_REQUEST,
                                "None of the flavors can satisfy storage request");
                    }

                    String sliceName = null;
                    StackContext context = new StackContext(sliceName, workflowId, region);

                    String networkId = null;
                    if (workflowNetwork != null && workflowNetwork.containsKey(NetworkController.NetworkId)) {
                        networkId = workflowNetwork.get(NetworkController.NetworkId);
                    } else {
                        networkId = networkController.getNetworkId(region, MobiusConfig.getInstance().getChameleonDefaultNetwork());
                    }

                    String prefix = request.getTarget() + CloudContext.StorageNameSuffix;
                    ComputeResponse response = context.provisionNode(flavorList, nameIndex, null, request.getLeaseEnd(),
                            prefix, StackContext.postBootScriptRequiredForStorage, metaData, networkId, null);
                    LOGGER.debug("Created new context=" + sliceName);
                    nameIndex = response.getNodeCount();

                    sliceName = context.getSliceName();
                    stackContextHashMap.put(sliceName, context);
                    LOGGER.debug("Added " + sliceName);
                    break;
                }
                case DELETE:
                {
                    // Find all contexts which have storage name containing target name
                    Set<StackContext> stackContextSet = new HashSet<>();
                    String prefix = request.getTarget() + CloudContext.StorageNameSuffix;
                    LOGGER.debug("hostNameSet: " + hostNameSet.size());
                    for(String h : hostNameSet) {
                        if(h.contains(prefix)) {
                            LOGGER.debug("Storage contexts to be deleted: " + h);
                            LOGGER.debug("hostNameToSliceNameHashMap: " + hostNameToSliceNameHashMap.size());

                            String sliceName = hostNameToSliceNameHashMap.get(h);
                            if(sliceName != null) {
                                LOGGER.debug("stackContextHashMap: " + stackContextHashMap.size());

                                stackContextSet.add(stackContextHashMap.get(sliceName));
                            }
                        }
                    }
                    LOGGER.debug("Storage contexts to be deleted: " + stackContextSet.size());
                    // Delete the storage and release all associated resources
                    for(StackContext stackContext: stackContextSet) {
                        stackContext.stop();
                        stackContextHashMap.remove(stackContext.getSliceName());
                    }
                    break;
                }
                case RENEW:
                {
                    // Find all contexts which have storage name containing target name
                    Set<StackContext> stackContextSet = new HashSet<>();
                    String prefix = request.getTarget() + CloudContext.StorageNameSuffix;
                    for(String h : hostNameSet) {
                        LOGGER.debug("Storage contexts to be renewed: " + h);
                        if(h.contains(prefix)) {
                            String sliceName = hostNameToSliceNameHashMap.get(h);
                            if(sliceName != null) {
                                stackContextSet.add(stackContextHashMap.get(sliceName));
                            }
                        }
                    }
                    LOGGER.debug("Storage contexts to be renewed: " + stackContextSet.size());
                    // Delete the storage and release all associated resources
                    for(StackContext stackContext: stackContextSet) {
                        stackContext.renew(request.getLeaseEnd());
                    }
                    break;
                }
            }
            LOGGER.debug("OUT nameIndex=" + nameIndex);
        }
        return nameIndex;
    }

    /*
     * @brief performs following periodic actions
     *        - Reload hostnames of all instances
     *        - Reload hostNameToSliceNameHashMap
     *        - Determine if notification to pegasus should be triggered
     *        - Build notification JSON object
     *
     * @return JSONObject representing notification for chameleon context to be sent to pegasus
     */
    @Override
    public JSONObject doPeriodic() {
        synchronized (this) {
            LOGGER.debug("IN");
            StackContext context = null;
            JSONObject retVal = null;
            JSONArray array = new JSONArray();
            LOGGER.debug("clearing maps");
            hostNameToSliceNameHashMap.clear();
            hostNameSet.clear();
            Iterator<HashMap.Entry<String, StackContext>> iterator = stackContextHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, StackContext> entry = iterator.next();
                context = entry.getValue();
                Set<String> hostNames = new HashSet<>();
                JSONObject result = null;
                result = context.doPeriodic(hostNames);

                hostNameSet.addAll(hostNames);
                for (String h : hostNames) {
                    if (!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                        hostNameToSliceNameHashMap.put(h, context.getSliceName());
                    }
                }
                if (result != null && !result.isEmpty()) {
                    array.add(result);
                }
                triggerNotification |= context.canTriggerNotification();
                if (context.canTriggerNotification()) {
                    context.setNotificationSent();
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }

            LOGGER.debug("OUT");
            return retVal;
        }
    }

    public String getNetworkVlanId() {
        LOGGER.debug("IN");
        String retVal = null;
        if(workflowNetwork != null) {
            Network network = networkController.getNetwork(region, workflowNetwork.get(NetworkController.NetworkId));
            if(network.getPhysicalNetworkName().compareToIgnoreCase(stitchablePhysicalNetwork) == 0) {
                if(network.getSegmentationId() != null) {
                    return network.getSegmentationId().toString();
                }
            }

        }
        LOGGER.debug("OUT");
        return retVal;
    }

    /*
     * @brief function to check get status for the context
     *
     * @return JSONObject representing status
     */
    @Override
    public JSONObject getStatus() {
        synchronized (this) {
            LOGGER.debug("IN");
            JSONObject retVal = null;
            JSONArray array = new JSONArray();

            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                JSONObject object = context.status(hostNameSet);
                if (!object.isEmpty()) {
                    array.add(object);
                }
                for (String h : hostNameSet) {
                    if (!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                        LOGGER.debug("Adding " + h + "=>" + context.getSliceName() + " to hostNameToSliceNameHashMap");
                        hostNameToSliceNameHashMap.put(h, context.getSliceName());
                    }
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }

            if(workflowNetwork != null) {
                String vlanId = getNetworkVlanId();
                if(vlanId != null) {
                    retVal.put(CloudContext.JsonKeyVlan, vlanId);
                }
            }
            LOGGER.debug("OUT");
            return retVal;
        }
    }

    /*
     * @brief function to check if an instance with this hostname exists in this context
     *
     * @return true if hostname exists; false otherwise
     */
    @Override
    public boolean containsSlice(String sliceName) {
        return stackContextHashMap.containsKey(sliceName);
    }

    /*
     * @brief function to release all resources associated with this context
     */
    @Override
    public void stop() {
        synchronized (this) {
            LOGGER.debug("IN");
            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                context.stop();
            }
            stackContextHashMap.clear();
            if(workflowNetwork != null) {
                networkController.deleteNetwork(region, workflowNetwork, 300);
                if(workflowNetwork.containsKey(NetworkLeaseId)) {
                    try {
                        api.deleteLease(region, workflowNetwork.get(NetworkLeaseId));
                    }
                    catch (Exception e) {
                        LOGGER.error("Exception occurred while deleting network lease e=" + e.getMessage());
                    }
                }
            }
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief function to process a stitch request; throws exception as it is not supported for chameleon
     *
     * @param request - stitch request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     *
     */
    @Override
    public int processStitchRequest(StitchRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for chameleon");
    }
    /*
     * @brief function to stitch to sdx and advertise a prefix for add operation and unstitch in case of delete
     *
     * @param hostname - hostname
     * @param ip - ip
     * @param subnet - subnet
     * @param localSubnet - localSubnet
     * @param action - action
     * @param destHostName - destHostName
     * @param sdxStitchPortInterfaceIP - sdxStitchPortInterfaceIP (used only for chameleon)
     *
     * @throws Exception in case of error
     *
     */
    @Override
    public void processNetworkRequestSetupStitchingAndRoute(String hostname, String ip, String subnet,
                                                            String localSubnet,
                                                            NetworkRequest.ActionEnum action, String destHostName,
                                                            String sdxStitchPortInterfaceIP) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN hostname=" + hostname + " ip=" + ip + " subnet=" + subnet
                    + " localSubnet=" + localSubnet
                    + " action=" + action
                    + " destHostName=" + destHostName + " hostNameToSliceNameHashMap="
                    + hostNameToSliceNameHashMap.toString());

            String sliceName = hostNameToSliceNameHashMap.get(hostname);
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap="
                        + hostNameToSliceNameHashMap.toString());
            }

            StackContext context = stackContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }

            String destSliceName = hostNameToSliceNameHashMap.get(destHostName);
            if (destSliceName != null) {
                if (sliceName.equalsIgnoreCase(destSliceName)) {
                    throw new MobiusException("destination and source cannot be in the same slice");
                }
                StackContext destContext = stackContextHashMap.get(destSliceName);
                if (destContext == null) {
                    throw new MobiusException("slice destContext not found");
                }
                if (destContext.getRegion().equalsIgnoreCase(context.getRegion())) {
                    throw new MobiusException("destination and source cannot be in same region");
                }
            }

            try {
                context.processNetworkRequestSetupStitchingAndRoute(hostname, getNetworkVlanId(), subnet, localSubnet,
                        action,
                        destHostName, sdxStitchPortInterfaceIP);
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }
    /*
     * @brief function to connect the link between source and destination subnet
     *
     * @param hostname - hostname
     * @param subnet1 - subnet1
     * @param subnet2 - subnet2
     * @param bandwidth - bandwidth
     * @param destinationIP - destinationIP
     * @param sdxStitchPortInterfaceIP - sdxStitchPortInterfaceIP (used only for chameleon)
     *
     * @throws Exception in case of error
     *
     */
    @Override
    public void processNetworkRequestLink(String hostname, String subnet1, String subnet2, String bandwidth,
                                          String destinationIP, String sdxStitchPortInterfaceIP) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN: hostname=" + hostname + " subnet1=" + subnet1 + " subnet2=" + subnet2 + " hostNameToSliceNameHashMap=" + hostNameToSliceNameHashMap.toString());
            String sliceName = hostNameToSliceNameHashMap.get(hostname);
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap=" + hostNameToSliceNameHashMap.toString());
            }
            StackContext context = stackContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }
            try {
                context.processNetworkRequestLink(hostname, subnet1, subnet2, bandwidth, destinationIP, sdxStitchPortInterfaceIP);
            }
            finally {
                LOGGER.debug("OUT");
            }
        }
    }

    public void processSdxPrefix(SdxPrefix request) throws Exception {
        synchronized (this) {
            String sliceName = hostNameToSliceNameHashMap.get(request.getSource());
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap="
                        + hostNameToSliceNameHashMap.toString());
            }

            StackContext context = stackContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }

            try {
                context.processSdxPrefix(request);
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }
}

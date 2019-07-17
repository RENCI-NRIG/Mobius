package org.renci.mobius.controllers.jetstream;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.NetworkController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.util.*;

/*
 * @brief class represents context for all resources on a specific region on jetstream. It maintains
 *        ReqContext per request received.
 *
 * @author kthare10
 */
public class JetstreamContext extends CloudContext  {
    private static final Logger LOGGER = Logger.getLogger( JetstreamContext.class.getName() );
    private static final Long maxDiffInSeconds = 604800L;

    private HashMap<String, ReqContext> stackContextHashMap;
    private String region;
    private String authUrl;
    private Map<String, String> workflowNetwork = null;


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
    public JetstreamContext(CloudContext.CloudType t, String s, String workflowId) throws Exception {
        super(t, s, workflowId);
        stackContextHashMap = new HashMap<>();
        String[] arrOfStr = s.split(":");
        if (arrOfStr.length < 2 || arrOfStr.length > 2) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
        }
        if(arrOfStr[1].compareToIgnoreCase("iu") == 0) {
            authUrl = MobiusConfig.getInstance().getJetStreamIuAuthUrl();
        }
        else if(arrOfStr[1].compareToIgnoreCase("tacc") == 0) {
            authUrl = MobiusConfig.getInstance().getJetStreamTaccAuthUrl();
        }
        else {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
        }

        region = "RegionOne";
    }

    /*
     * @brief setup private network for jetstream region; all instances are connected to each other via this
     *        network;
     */
    private Pair<String, String> setupNetwork(ComputeRequest request) throws Exception {

        NetworkController networkController = null;
        String networkId = null, sgName = null;
        try {

            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();

            networkController = new NetworkController(authUrl, user, password, userDomain, project);

            if (workflowNetwork != null &&
                    workflowNetwork.containsKey(NetworkController.NetworkId) &&
                    workflowNetwork.containsKey(NetworkController.SecurityGroupId)) {
                return Pair.of(workflowNetwork.get(NetworkController.NetworkId),
                        networkController.getSecurityGroupName(region, workflowNetwork.get(NetworkController.SecurityGroupId)));
            }

            // If network type is default; throw exception
            if (request.getNetworkType() == ComputeRequest.NetworkTypeEnum.DEFAULT) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Network Type is required and cannot be default for jetstream");
            }

            String externalNetworkId = networkController.getNetworkId(region, request.getExternalNetwork());
            // Workflow network for region does not exist create workflow private network
            workflowNetwork = networkController.createNetwork(region, null,
                    externalNetworkId, false, request.getNetworkCidr(), workflowId, true);

            networkId = workflowNetwork.get(NetworkController.NetworkId);
            sgName = networkController.getSecurityGroupName(region, workflowNetwork.get(NetworkController.SecurityGroupId));
        }
        catch (Exception e){
            LOGGER.error("Exception occured while setting up network ");
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
            throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to setup network " + e.getLocalizedMessage());
        }
        finally {
            if(networkController != null) {
                networkController.close();
            }
            LOGGER.debug("status: OUT");
        }
        return Pair.of(networkId, sgName);
    }
    /*
     * @brief add cloud specific info to JSON Object representing JetstreamContext;
     *        JSON Object is saved to database
     *
     * @param object - json object representing JetstreamContext
     */
    @Override
    public JSONObject addCloudSpecificDataToJson(JSONObject object) {
        if(workflowNetwork != null) {
            String networkId = workflowNetwork.get(NetworkController.NetworkId);
            String subnetId = workflowNetwork.get(NetworkController.SubnetId);
            String routerId = workflowNetwork.get(NetworkController.RouterId);
            String securityGroupId = workflowNetwork.get(NetworkController.SecurityGroupId);

            if(networkId != null) {
                object.put(NetworkController.NetworkId, networkId);
            }
            if(subnetId != null) {
                object.put(NetworkController.SubnetId, subnetId);
            }
            if(routerId != null) {
                object.put(NetworkController.RouterId, routerId);
            }
            if(securityGroupId != null) {
                object.put(NetworkController.SecurityGroupId, securityGroupId);
            }
        }
        return object;
    }

    /*
     * @brief function to load cloud specific data from JSON Object representing JetstreamContext
     *
     * @param object - json object representing JetstreamContext
     */
    @Override
    public void loadCloudSpecificDataFromJson(JSONObject object) {
        String networkId = (String) object.get(NetworkController.NetworkId);
        String subnetId = (String) object.get(NetworkController.SubnetId);
        String routerId = (String) object.get(NetworkController.RouterId);
        String securityGroupId = (String) object.get(NetworkController.SecurityGroupId);
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
        if(securityGroupId != null) {
            workflowNetwork.put(NetworkController.SecurityGroupId, securityGroupId);
        }
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
        LOGGER.debug("validateComputeRequest: IN");

        if(request.getCpus() == 0 ) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "No cpus are requested");
        }

        if(request.getGpus() != 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Gpus not supported");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.DEFAULT) {
            throw new MobiusException(HttpStatus.BAD_REQUEST,
                        "Network type cannot be default for jetstream");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.PRIVATE) {
            if(request.getExternalNetwork() == null || request.getNetworkCidr() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST,
                        "Required externalNetwork or networkCidr not specified for private network");
            }
        }

        if(request.getHostNamePrefix() != null && !request.getHostNamePrefix().matches("[a-zA-Z]+")) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Host Name prefix can only contain alphabet characters");
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, maxDiffInSeconds);
        LOGGER.debug("validateComputeRequest: OUT");
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
        LOGGER.debug("validateStorageRequest: IN");
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, maxDiffInSeconds);
        LOGGER.debug("validateStorageRequest: OUT");
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
                ReqContext context = null;
                for (HashMap.Entry<String, ReqContext> entry : stackContextHashMap.entrySet()) {
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
                    LOGGER.debug("fromJson(): sliceName=" + sliceName);
                    ReqContext sliceContext = new ReqContext(sliceName, workflowId, region, authUrl);
                    sliceContext.fromJson(slice);
                    stackContextHashMap.put(sliceName, sliceContext);
                }
            } else {
                LOGGER.error("fromJson(): Null array passed");
            }
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
     * @return number representing index to be added for the instance name
     */
    @Override
    public Pair<Integer, Integer> processCompute(ComputeRequest request, int nameIndex, int spNameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            validateComputeRequest(request, isFutureRequest);

            Map<String, Integer> flavorList = JetstreamFlavorAlgo.determineFlavors(request.getCpus(),
                    request.getRamPerCpus(), request.getDiskPerCpus());

            if (flavorList == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
            }

            String sliceName = null;
            ReqContext context = new ReqContext(sliceName, workflowId, region, authUrl);

            try {
                Pair<String, String> nwIdSgName = setupNetwork(request);
                // For jetstream; slice per request mechanism is followed
                nameIndex = context.provisionNode( flavorList, nameIndex, request.getImageName(),
                        request.getLeaseEnd(), request.getHostNamePrefix(), request.getPostBootScript(),
                        null, nwIdSgName.getFirst(), request.getIpAddress(), nwIdSgName.getSecond());
                LOGGER.debug("Created new context=" + sliceName);

                sliceName = context.getSliceName();
                stackContextHashMap.put(sliceName, context);
                LOGGER.debug("Added " + sliceName);

                return Pair.of(nameIndex, spNameIndex);
            } finally {
                LOGGER.debug("processCompute: OUT");
            }
        }
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
            validateStorageRequest(request, isFutureRequest);

            String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap");
            }

            switch (request.getAction()) {
                case ADD: {
                    try {
                        ReqContext context = stackContextHashMap.get(sliceName);
                        nameIndex = context.addStorage(request, nameIndex);
                    } finally {
                        LOGGER.debug("processStorage: OUT");
                    }
                    break;
                }
                case DELETE:
                {
                    try {
                        ReqContext context = stackContextHashMap.get(sliceName);
                        context.deleteStorage(request);
                    } finally {
                        LOGGER.debug("processStorage: OUT");
                    }
                    break;
                }
                case RENEW:
                {
                    
                    break;
                }
            }
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
            LOGGER.debug("doPeriodic: IN");
            ReqContext context = null;
            JSONObject retVal = null;
            JSONArray array = new JSONArray();
            hostNameToSliceNameHashMap.clear();
            hostNameSet.clear();
            Iterator<HashMap.Entry<String, ReqContext>> iterator = stackContextHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, ReqContext> entry = iterator.next();
                context = entry.getValue();

                if(context.hasExpired()) {
                    context.stop();
                    handleExpiredRequest(context.getSliceName());
                    iterator.remove();
                    continue;
                }

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

            LOGGER.debug("doPeriodic: OUT");
            return retVal;
        }
    }

    /*
     * @brief function to check get status for the context
     *
     * @return JSONObject representing status
     */
    @Override
    public JSONObject getStatus() {
        synchronized (this) {
            LOGGER.debug("getStatus: IN");
            JSONObject retVal = null;
            JSONArray array = new JSONArray();

            ReqContext context = null;
            for (HashMap.Entry<String, ReqContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                JSONObject object = context.status(hostNameSet);
                if (!object.isEmpty()) {
                    array.add(object);
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }
            LOGGER.debug("getStatus: OUT");
            return retVal;        }
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
            LOGGER.debug("stop: IN");
            ReqContext context = null;
            for (HashMap.Entry<String, ReqContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                context.stop();
            }
            stackContextHashMap.clear();

            if(workflowNetwork != null) {
                String user = MobiusConfig.getInstance().getJetStreamUser();
                String password = MobiusConfig.getInstance().getJetStreamUserPassword();
                String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
                String project = MobiusConfig.getInstance().getJetStreamProject();

                NetworkController networkController = new NetworkController(authUrl, user, password, userDomain, project);
                networkController.deleteNetwork(region, workflowNetwork, 300);
            }
            LOGGER.debug("stop: OUT");
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
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for jetstream");
    }

    /*
     * @brief function to handle Expired Request
     *
     * @param sliceName - slice name
     */
    protected void handleExpiredRequest(String sliceName) {
        LOGGER.debug("handleExpiredRequest: IN");
        if(sliceName != null && hostNameToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<HashMap.Entry<String, String>> iterator = hostNameToSliceNameHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        LOGGER.debug("handleExpiredRequest: OUT");
    }
    /*
     * @brief function to process a network request;
     *
     * @param hostname - hostname
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     *
     * @throws Exception in case of error
     *
     */
    @Override
    public void processNetworkRequestSetupStitchingAndRoute(String hostname, String ip, String subnet, NetworkRequest.ActionEnum action) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for jetsream");
    }
    /*
     * @brief function to process a network request;
     *
     * @param hostname - hostname
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     *
     * @throws Exception in case of error
     *
     */
    @Override
    public void processNetworkRequestLink(String hostname, String subnet1, String subnet2) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for jetsream");
    }
}

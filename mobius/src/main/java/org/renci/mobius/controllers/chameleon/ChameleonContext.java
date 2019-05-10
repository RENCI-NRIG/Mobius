package org.renci.mobius.controllers.chameleon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.NetworkController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

/*
 * @brief class represents context for all resources on a specific region on chameleon. It maintains
 *        StackContext per request received.
 *
 * @author kthare10
 */
public class ChameleonContext extends CloudContext {
    private static final Logger LOGGER = Logger.getLogger( ChameleonContext.class.getName() );
    private static final Long maxDiffInSeconds = 604800L;

    private HashMap<String, StackContext> stackContextHashMap;
    private Map<String, String> workflowNetwork = null;
    private Map<String, String> metaData = null;
    private String region = null;

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
    }

    /*
     * @brief setup private network for chameleon region; all instances are connected to each other via this
     *        network;
     */
    private String setupNetwork(ComputeRequest request) throws Exception {

        NetworkController networkController = null;
        String networkId = null;
        try {

            if (workflowNetwork != null && workflowNetwork.containsKey(NetworkController.NetworkId)) {
                return workflowNetwork.get(NetworkController.NetworkId);
            }

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();

            networkController = new NetworkController(authurl, user, password, userDomain, project);

            // If network type is default; use chameleon default network i.e. sharednet1
            if (request.getNetworkType() == ComputeRequest.NetworkTypeEnum.DEFAULT) {
                return networkController.getNetworkId(region, MobiusConfig.getInstance().getChameleonDefaultNetwork());
            }

            String externalNetworkId = networkController.getNetworkId(region, request.getExternalNetwork());
            // Workflow network for region does not exist create workflow private network
            workflowNetwork = networkController.createNetwork(region, request.getPhysicalNetwork(),
                    externalNetworkId, false, request.getNetworkCidr(), workflowId);

            networkId = workflowNetwork.get(NetworkController.NetworkId);
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
        LOGGER.debug("validateComputeRequest: IN");

        if(request.getCpus() == 0 && request.getGpus() == 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "No cpus and gpus are requested");
        }

        if(request.getIpAddress() != null) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "IP address allocation not allowed");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.PRIVATE) {
            if(request.getPhysicalNetwork() == null || request.getExternalNetwork() == null ||
                    request.getNetworkCidr() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST,
                        "Required externalNetwork, physicalNetwork or networkCidr not specified for private network");
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
                    LOGGER.debug("fromJson(): sliceName=" + sliceName);
                    StackContext sliceContext = new StackContext(sliceName, workflowId, region);
                    sliceContext.fromJson(slice);
                    stackContextHashMap.put(sliceName, sliceContext);
                }
            } else {
                LOGGER.error("fromJson(): Null array passed");
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
            String networkId = workflowNetwork.get(NetworkController.NetworkId);
            String subnetId = workflowNetwork.get(NetworkController.SubnetId);
            String routerId = workflowNetwork.get(NetworkController.RouterId);
            if(networkId != null) {
                object.put(NetworkController.NetworkId, networkId);
            }
            if(subnetId != null) {
                object.put(NetworkController.SubnetId, subnetId);
            }
            if(routerId != null) {
                object.put(NetworkController.RouterId, routerId);
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
        String networkId = (String) object.get(NetworkController.NetworkId);
        String subnetId = (String) object.get(NetworkController.SubnetId);
        String routerId = (String) object.get(NetworkController.RouterId);
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

            Map<String, Integer> flavorList = ChameleonFlavorAlgo.determineFlavors(request.getCpus(), request.getGpus(),
                    request.getRamPerCpus(), request.getDiskPerCpus());

            if (flavorList == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
            }

            String sliceName = null;
            StackContext context = new StackContext(sliceName, workflowId, region);

            try {
                String networkId = setupNetwork(request);
                // For chameleon; slice per request mechanism is followed
                nameIndex = context.provisionNode( flavorList, nameIndex, request.getImageName(),
                        request.getLeaseEnd(), request.getHostNamePrefix(), request.getPostBootScript(),
                        metaData, networkId);
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
            NetworkController networkController = null;
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

                    try {

                        String networkId = null;
                        if (workflowNetwork != null && workflowNetwork.containsKey(NetworkController.NetworkId)) {
                            networkId = workflowNetwork.get(NetworkController.NetworkId);
                        } else {

                            String user = MobiusConfig.getInstance().getChameleonUser();
                            String password = MobiusConfig.getInstance().getChameleonUserPassword();
                            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
                            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
                            String project = MobiusConfig.getInstance().getChameleonProject();

                            networkController = new NetworkController(authurl, user, password, userDomain, project);
                            networkId = networkController.getNetworkId(region, MobiusConfig.getInstance().getChameleonDefaultNetwork());
                        }

                        String prefix = request.getTarget() + CloudContext.StorageNameSuffix;
                        nameIndex = context.provisionNode(flavorList, nameIndex, null, request.getLeaseEnd(),
                                prefix, StackContext.postBootScriptRequiredForStorage, metaData, networkId);
                        LOGGER.debug("Created new context=" + sliceName);

                        sliceName = context.getSliceName();
                        stackContextHashMap.put(sliceName, context);
                        LOGGER.debug("Added " + sliceName);

                    } finally {
                        if (networkController != null) {
                            networkController.close();
                        }
                        LOGGER.debug("processStorage: OUT");
                    }
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
            StackContext context = null;
            JSONObject retVal = null;
            JSONArray array = new JSONArray();
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

            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
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
            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                context.stop();
            }
            stackContextHashMap.clear();
            if(workflowNetwork != null) {
                String user = MobiusConfig.getInstance().getChameleonUser();
                String password = MobiusConfig.getInstance().getChameleonUserPassword();
                String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
                String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
                String project = MobiusConfig.getInstance().getChameleonProject();

                NetworkController networkController = new NetworkController(authurl, user, password, userDomain, project);
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
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for chameleon");
    }
}

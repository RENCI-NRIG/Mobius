package org.renci.mobius.controllers.aws;

import com.amazonaws.services.ec2.model.InstanceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.ComputeResponse;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.*;
import org.springframework.http.HttpStatus;

import java.util.*;

/*
 * @brief class represents context for all resources on a specific region on mos. It maintains
 *        ReqContext per request received.
 *
 * @author kthare10
 */
public class AwsContext extends CloudContext {
    private static final Logger LOGGER = LogManager.getLogger( AwsContext.class.getName() );
    private static final Long maxDiffInSeconds = 604800L;

    private HashMap<String, ReqContext> stackContextHashMap;
    private String region;
    private String accessId;
    private String secretKey;
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
    public AwsContext(CloudContext.CloudType t, String s, String workflowId) throws Exception {
        super(t, s, workflowId);
        stackContextHashMap = new HashMap<>();
        String[] arrOfStr = s.split(":");
        if (arrOfStr.length < 2 || arrOfStr.length > 2) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
        }
        region = arrOfStr[1];
        accessId = MobiusConfig.getInstance().getAwsAccessId();
        secretKey = MobiusConfig.getInstance().getAwsSecreteKey();
    }

    /*
     * @brief setup private network for mos region; all instances are connected to each other via this
     *        network;
     */
    private void setupNetwork(ComputeRequest request) throws Exception {
        LOGGER.debug("IN request=" + request);
        AwsEc2Api awsEc2Api = null;
        try {
            awsEc2Api = new AwsEc2Api(region, accessId, secretKey);

            if (workflowNetwork != null) {
                return;
            }

            String publicKeyFile = MobiusConfig.getInstance().getAwsUserSshKey();
            String networkName = workflowId + CloudContext.generateRandomString();
            workflowNetwork = awsEc2Api.setupNetwork(networkName, request.getVpcCidr(),
                    request.getExternalNetwork(), request.getNetworkCidr(), publicKeyFile);
        }
        catch (Exception e){
            workflowNetwork = null;
            LOGGER.error("Exception occured while setting up network ");
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
            throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to setup network " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }
    /*
     * @brief add cloud specific info to JSON Object representing AwsContext;
     *        JSON Object is saved to database
     *
     * @param object - json object representing AwsContext
     */
    @Override
    public JSONObject addCloudSpecificDataToJson(JSONObject object) {
        if(workflowNetwork != null) {
            String vpcId = workflowNetwork.get(AwsEc2Api.VpcId);
            String extSubnetId = workflowNetwork.get(AwsEc2Api.ExternalSubnetId);
            String intSubnetId = workflowNetwork.get(AwsEc2Api.InternalSubnetId);
            String routeTableId = workflowNetwork.get(AwsEc2Api.RouteTableId);
            String internetGatewayId = workflowNetwork.get(AwsEc2Api.InternalSubnetId);
            String associationId = workflowNetwork.get(AwsEc2Api.AssociationId);
            String keyPairId = workflowNetwork.get(AwsEc2Api.KeyPairId);
            String securityGroupId = workflowNetwork.get(AwsEc2Api.SecurityGroupId);

            if(vpcId != null) {
                object.put(AwsEc2Api.VpcId, vpcId);
            }
            if(extSubnetId != null) {
                object.put(AwsEc2Api.ExternalSubnetId, extSubnetId);
            }
            if(intSubnetId != null) {
                object.put(AwsEc2Api.InternalSubnetId, intSubnetId);
            }
            if(routeTableId != null) {
                object.put(AwsEc2Api.RouteTableId, routeTableId);
            }
            if(internetGatewayId != null) {
                object.put(AwsEc2Api.InternetGatewayId, internetGatewayId);
            }
            if(associationId != null) {
                object.put(AwsEc2Api.AssociationId, associationId);
            }
            if(keyPairId != null) {
                object.put(AwsEc2Api.KeyPairId, keyPairId);
            }
            if(securityGroupId != null) {
                object.put(AwsEc2Api.SecurityGroupId, securityGroupId);
            }
        }
        return object;
    }

    /*
     * @brief function to load cloud specific data from JSON Object representing AwsContext
     *
     * @param object - json object representing AwsContext
     */
    @Override
    public void loadCloudSpecificDataFromJson(JSONObject object) {
        String vpcId = (String)object.get(AwsEc2Api.VpcId);
        String extSubnetId = (String)object.get(AwsEc2Api.ExternalSubnetId);
        String intSubnetId = (String)object.get(AwsEc2Api.InternalSubnetId);
        String routeTableId = (String)object.get(AwsEc2Api.RouteTableId);
        String internetGatewayId = (String)object.get(AwsEc2Api.InternalSubnetId);
        String associationId = (String)object.get(AwsEc2Api.AssociationId);
        String keyPairId = (String)object.get(AwsEc2Api.KeyPairId);
        String securityGroupId = (String)object.get(AwsEc2Api.SecurityGroupId);

        if(vpcId != null) {
            workflowNetwork = new HashMap<>();
            workflowNetwork.put(AwsEc2Api.VpcId, vpcId);
        }
        if(extSubnetId != null) {
            workflowNetwork.put(AwsEc2Api.ExternalSubnetId, extSubnetId);
        }
        if(intSubnetId != null) {
            workflowNetwork.put(AwsEc2Api.InternalSubnetId, intSubnetId);
        }
        if(routeTableId != null) {
            workflowNetwork.put(AwsEc2Api.RouteTableId, routeTableId);
        }
        if(internetGatewayId != null) {
            workflowNetwork.put(AwsEc2Api.InternetGatewayId, internetGatewayId);
        }
        if(associationId != null) {
            workflowNetwork.put(AwsEc2Api.AssociationId, associationId);
        }
        if(keyPairId != null) {
            workflowNetwork.put(AwsEc2Api.KeyPairId, keyPairId);
        }
        if(securityGroupId != null) {
            workflowNetwork.put(AwsEc2Api.SecurityGroupId, securityGroupId);
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
        LOGGER.debug("IN request=" + request + " isFutureRequest=" + isFutureRequest);

        if(request.getCpus() == 0 && request.getGpus() == 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "No cpus/gpus are requested");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.DEFAULT) {
            throw new MobiusException(HttpStatus.BAD_REQUEST,
                    "Network type cannot be default for aws");
        }

        if(request.getNetworkType() == ComputeRequest.NetworkTypeEnum.PRIVATE) {
            if(request.getVpcCidr() == null || request.getNetworkCidr() == null || request.getExternalNetwork() == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST,
                        "Required vpcCidr or externalCidr or networkCidr not specified for private network");
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
        LOGGER.debug("IN");
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
                    ReqContext sliceContext = new ReqContext(sliceName, workflowId, region);
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
     * @return ComputeResponse
     */
    @Override
    public ComputeResponse processCompute(ComputeRequest request, int nameIndex, int spNameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN request=" + request + " nameIndex=" + nameIndex + " spIndex=" + spNameIndex + " isFutureRequest=" + isFutureRequest);
            validateComputeRequest(request, isFutureRequest);

            Map<InstanceType, Integer> flavorList = AwsFlavorAlgo.determineFlavors(request.getCpus(), request.getGpus(),
                    request.getRamPerCpus(), 0, request.getDiskPerCpus());

            if (flavorList == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
            }

            String sliceName = null;
            ReqContext context = new ReqContext(sliceName, workflowId, region);

            try {
                setupNetwork(request);

                // For aws; slice per request mechanism is followed
                ComputeResponse response = context.provisionNode( flavorList, nameIndex, request.getImageName(),
                        request.getHostNamePrefix(), request.getPostBootScript(), request.getIpAddress(),
                        workflowNetwork.get(AwsEc2Api.KeyPairId), workflowNetwork.get(AwsEc2Api.SecurityGroupId),
                        workflowNetwork.get(AwsEc2Api.ExternalSubnetId), workflowNetwork.get(AwsEc2Api.InternalSubnetId),
                        request.getLeaseEnd());
                LOGGER.debug("Created new context=" + sliceName);

                sliceName = context.getSliceName();
                stackContextHashMap.put(sliceName, context);
                LOGGER.debug("Added " + sliceName);
                response.setStitchCount(spNameIndex);
                return response;
            } finally {
                LOGGER.debug("OUT nameIndex=" + nameIndex + " spIndex=" + spNameIndex);
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
            LOGGER.debug("IN request=" + request + " nameIndex=" + nameIndex + " isFutureRequest=" + isFutureRequest);
            validateStorageRequest(request, isFutureRequest);

            String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap");
            }

            switch (request.getAction()) {
                case ADD: {
                    ReqContext context = stackContextHashMap.get(sliceName);
                    // TODO
                    break;
                }
                case DELETE: {
                    ReqContext context = stackContextHashMap.get(sliceName);
                    // TODO
                    break;
                }
                case RENEW:
                {

                    break;
                }
            }
        }
        LOGGER.debug("OUT nameIndex=" + nameIndex);
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

            LOGGER.debug("OUT");
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
            LOGGER.debug("IN");
            JSONObject retVal = null;
            JSONArray array = new JSONArray();

            ReqContext context = null;
            for (HashMap.Entry<String, ReqContext> entry : stackContextHashMap.entrySet()) {
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
            try {
                LOGGER.debug("IN");
                ReqContext context = null;
                for (HashMap.Entry<String, ReqContext> entry : stackContextHashMap.entrySet()) {
                    context = entry.getValue();
                    context.stop();
                }
                stackContextHashMap.clear();

                if (workflowNetwork != null) {
                    AwsEc2Api awsEc2Api = new AwsEc2Api(region, accessId, secretKey);
                    awsEc2Api.tearNetwork(workflowNetwork);
                }
            }
            catch (Exception e) {
                LOGGER.error("Exception occured e= " + e);
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
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for mos");
    }

    /*
     * @brief function to handle Expired Request
     *
     * @param sliceName - slice name
     */
    protected void handleExpiredRequest(String sliceName) {
        LOGGER.debug("IN");
        if(sliceName != null && hostNameToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<HashMap.Entry<String, String>> iterator = hostNameToSliceNameHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        LOGGER.debug("OUT");
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
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for aws");
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
    public void processNetworkRequestLink(String hostname, String subnet1, String subnet2, String bandwidth, String destinationIP, String sdxStitchPortInterfaceIP) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for aws");
    }

    public void processSdxPrefix(SdxPrefix request) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported for aws");
    }
}


package org.renci.mobius.controllers.aws;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.ComputeResponse;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;

import java.util.*;

/*
 * @brief class representing resources associated with a single mobius request
 * @author kthare10
 */
public class ReqContext {
    private static final Logger LOGGER = LogManager.getLogger( ReqContext.class.getName() );

    private String sliceName;
    private String workflowId;
    private int activeOrFailedInstances;
    private List<Pair<String, String>> instanceIdList;
    private String leaseId;
    private String region;
    private String authUrl;
    private boolean notificationSent;
    private Date leaseEnd;
    private String accessId;
    private String secretKey;
    /*
     * @brief constructor
     *
     * @param sliceName - slice name
     * @param workflowId - workflow id
     * @param region - chameleon region on which resources are allocated
     *
     */
    public ReqContext(String sliceName, String workflowId, String region) {
        this.sliceName = sliceName;
        this.workflowId = workflowId;
        activeOrFailedInstances = 0;
        instanceIdList = new LinkedList<>();
        leaseId = null;
        this.region = region;
        this.authUrl = authUrl;
        notificationSent = false;
        leaseEnd = null;
        accessId = MobiusConfig.getInstance().getAwsAccessId();
        secretKey = MobiusConfig.getInstance().getAwsSecreteKey();
    }

    /*
     * @brief returns slice name
     *
     * @return slice name
     */
    public String getSliceName() {
        return sliceName;
    }

    /*
     * @brief set notification sent flag
     */
    public void setNotificationSent() { notificationSent = true; }

    /*
     * @brief determine if notification should be triggered
     *
     * @return true if notification should be triggered; false otherwise
     *
     */
    public boolean canTriggerNotification() {
        if(!notificationSent && (activeOrFailedInstances > 0 || activeOrFailedInstances == instanceIdList.size())) {
            return true;
        }
        return false;
    }

    /*
     * @brief determine if lease has expired
     *
     * @return true if lease has expired; false otherwise
     *
     */
    public boolean hasExpired() {
        if(leaseEnd != null) {
            Date now = new Date();
            if( now.compareTo(leaseEnd) >= 0 ) {
                return true;
            }
        }
        return false;
    }

    /*
     * @brief function to generate JSONobject representing this contexts
     *
     * @return JSONObject
     */
    public JSONObject toJson() {
        synchronized (this) {
            JSONObject retVal = new JSONObject();
            retVal.put("name", sliceName);
            if(leaseId != null) {
                retVal.put("leaseId", leaseId);
            }
            if(instanceIdList.size() > 0) {
                JSONArray ids = new JSONArray();
                for (Pair<String, String> instance : instanceIdList) {
                    JSONObject id = new JSONObject();
                    id.put("id", instance.getFirst() + "==" + instance.getSecond());
                    ids.add(id);
                }
                retVal.put("ids", ids);
            }
            return retVal;
        }
    }

    /*
     * @brief construct context from JSONObject read from database representing the context
     *
     * @param object - json object representing context
     */
    public void fromJson(JSONObject object) {
        synchronized (this) {
            sliceName = (String) object.get("name");
            if (object.get("leaseId") != null) {
                leaseId = (String) object.get("leaseId");
            }
            if (object.get("ids") != null) {
                JSONArray ids = (JSONArray) object.get("ids");
                for (Object id : ids) {
                    JSONObject instanceId = (JSONObject) id;
                    String instanceIdNetworkIdPair = (String) instanceId.get("id");
                    String[] result = instanceIdNetworkIdPair.split("==");
                    instanceIdList.add(Pair.of(result[0], result[1]));
                }
            }
        }
    }

    /*
     * @brief function to release all resources associated with this context
     */
    public void stop() {
        LOGGER.debug("IN");
        LOGGER.debug("Instance destruction taking plance =============================");

        try {
            LOGGER.debug("Successfully deleted slice " + sliceName);

            AwsEc2Api awsEc2Api = new AwsEc2Api(region, accessId, secretKey);
            for(Pair<String, String> instance : instanceIdList) {
                try {
                    awsEc2Api.deleteInstance(instance.getFirst());
                    awsEc2Api.deleteNetworkInterface(instance.getSecond());
                }
                catch (Exception e) {
                    LOGGER.debug("Ignoring exception during destroy e=" + e);
                }
            }
        }
        catch (Exception e){
            LOGGER.debug("Exception occured while deleting slice " + sliceName);
        }
        LOGGER.debug("Instance destruction taking plance =============================");
        LOGGER.debug("OUT");
    }

    /*
     * @brief function to provision a node on chameleon; if no post boot script is specified
     *        default post boot script to install neuca tool is passed; creates a lease; waits for lease
     *        to be active and then provisions the node. if lease does not become active in a timeout,
     *        request is treated as a failure
     *
     * @param flavorList - map of <flavorname, number of nodes for the flavor> to be instantiated
     * @param nameIndex - number representing index to be added to instance name
     * @param image - image name
     * @param leaseEnd - lease end time
     * @param hostNamePrefix - host name prefix
     * @param postBootScript - post boot script
     * @param metaData - meta data
     * @param networkId - network id to which instance is connected
     * @param ip - ip
     * @param sgName - sgName
     *
     * @return ComputeResponse
     * @throws Exception in case of error
     *
     */
    public ComputeResponse provisionNode(Map<InstanceType, Integer> flavorList, int nameIndex, String image,
                                         String hostNamePrefix, String postBootScript, String ip,
                                         String keyPairId, String securityGroupId, String extSubnet, String intSubnet,
                                         String leaseEnd) throws Exception {

        LOGGER.debug("IN flavorList=" + flavorList.toString() + " nameIndex=" + nameIndex + " image=" + image
                + " hostNamePrefix=" + hostNamePrefix + " postBootScript=" + postBootScript + " ip=" + ip
                + " keyPairId=" + keyPairId + " securityGroupId=" + securityGroupId + " extSubnet=" + extSubnet
                + " intSubnet=" + intSubnet + " leaseEnd=" + leaseEnd);

        try {

            AwsEc2Api awsEc2Api = new AwsEc2Api(region, accessId, secretKey);
            ComputeResponse response = new ComputeResponse(0,0);

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Aws, null);
            }

            // Extract image name
            if(image == null) {
                image = MobiusConfig.getInstance().getAwsDefaultImage();
            }

            this.leaseEnd = new Date();
            if(leaseEnd != null) {
                this.leaseEnd = new Date(Long.parseLong(leaseEnd) * 1000);
            }
            else {
                this.leaseEnd.setTime(this.leaseEnd.getTime() + 86400000);
            }

            for(Map.Entry<InstanceType, Integer> entry : flavorList.entrySet()) {
                for (int i = 0; i < entry.getValue(); ++i) {
                    String name = workflowId + "-";
                    if (hostNamePrefix != null) {
                        if (hostNamePrefix.contains(workflowId)) {
                            name = hostNamePrefix + nameIndex;
                        } else {
                            name = name + hostNamePrefix + nameIndex;
                        }
                    } else {
                        name = name + CloudContext.NodeName + nameIndex;
                    }
                    name = name.toLowerCase();
                    response.addHost(name, null);
                    LOGGER.debug("adding node=" + name + " with flavor=" + entry.getKey());


                    String instanceId = awsEc2Api.createInstance(name, keyPairId, securityGroupId, extSubnet,
                            postBootScript, image, entry.getKey());

                    if (instanceId == null) {
                        throw new MobiusException("Failed to create instance");
                    }

                    String networkInterfaceId = awsEc2Api. associateNetworkWithInstance(intSubnet, instanceId);
                    instanceIdList.add(Pair.of(instanceId, networkInterfaceId));
                    ++nameIndex;
                }
            }
            response.setNodeCount(nameIndex);
            return response;
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            // Cleanup in case of failure
            stop();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            // Cleanup in case of failure
            stop();
            throw new MobiusException("Failed to server compute request e=" + e.getMessage());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief function to generate JSONObject representing a single server
     *
     * @param server - server provisioned
     * @param ip - ip associated with server
     * @param fixedIPs - fixedIPs associated with server
     *
     * @return JSONObject representing server
     */
    private JSONObject nodeToJson(Instance server, String ip, List<String> fixedIPs){
        LOGGER.debug("IN server=" + server.toString() + " ip=" + ip);
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, server.getTags().get(0).getValue());
        object.put(CloudContext.JsonKeyState, server.getState().getName());
        if(ip != null) {
            object.put(CloudContext.JsonKeyPublicIP, ip);
        }
        else {
            object.put(CloudContext.JsonKeyPublicIP, "");
        }
        if(fixedIPs != null) {
            int index = 1;
            for(String i: fixedIPs) {
                object.put(CloudContext.JsonKeyIP + Integer.toString(index), i);
            }
        }
        LOGGER.debug("OUT");
        return object;
    }

    /*
     * @brief function to generate JSONObject representing status of all resources associated with this contexts
     *
     * @param hostNameSet - hostname set
     *
     * @return JSONObject representing status of context
     */
    public JSONObject status(Set<String> hostNameSet) {
        LOGGER.debug("IN hostNameSet=" + hostNameSet);
        JSONObject returnValue = new JSONObject();
        try {
            AwsEc2Api awsEc2Api = new AwsEc2Api(region, accessId, secretKey);
            returnValue.put(CloudContext.JsonKeySlice, sliceName);
            JSONArray array = new JSONArray();

            for(Pair<String, String> instanceIdPair : instanceIdList) {
                try {

                    Instance instance = awsEc2Api.getInstance(instanceIdPair.getFirst());
                    if(instance != null) {
                        String ip = null;
                        List<String> fixedIPs = new LinkedList<>();

                        if (!hostNameSet.contains(instance.getTags().get(0).getValue())) {
                            LOGGER.debug("Adding hostname: " + instance.getTags().get(0).getValue());
                            hostNameSet.add(instance.getTags().get(0).getValue());
                        }

                        if (instance.getState().getName().compareToIgnoreCase("running") == 0) {
                            ip = instance.getPublicIpAddress();
                            for(InstanceNetworkInterface networkInterface: instance.getNetworkInterfaces()) {
                                if(networkInterface.getNetworkInterfaceId().compareToIgnoreCase(instanceIdPair.getSecond()) == 0) {
                                    fixedIPs.add(networkInterface.getPrivateIpAddress());
                                }
                            }

                        } else if (instance.getState().getName().compareToIgnoreCase("pending") != 0) {
                            activeOrFailedInstances++;
                        }
                        JSONObject object = nodeToJson(instance, ip, fixedIPs);
                        array.add(object);
                    }
                    else {
                        LOGGER.error("Instance not found for InstanceId=" + instanceIdPair.getFirst());
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Exception occured while checking status e=" + e);
                    e.printStackTrace();
                }
            }

            returnValue.put(CloudContext.JsonKeyNodes, array);
        }
        catch (Exception e){
            LOGGER.error("Exception occured while getting status of slice " + sliceName);
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("OUT");
        }
        return returnValue;
    }

    /*
     * @brief function to periodically check status of all resources associated with context; assign floating ips to
     *        any new active instances
     *
     * @param hostNameSet - hostname set
     *
     * @return JSONObject representing status of context
     */
    public JSONObject doPeriodic(Set<String> hostNameSet) {
        LOGGER.debug("IN hostNameSet=" + hostNameSet);

        JSONObject object = null;
        try {
            object = status(hostNameSet);
        }
        catch (Exception e){
            LOGGER.error("Exception occured while performing periodic updates to slice " + sliceName);
        }
        LOGGER.debug("OUT");
        return object;
    }
}



package org.renci.mobius.controllers.jetstream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.ComputeController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.StorageRequest;

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
    private List<String> instanceIdList;
    private String leaseId;
    private String region;
    private String authUrl;
    private boolean notificationSent;
    private Date leaseEnd;

    /*
     * @brief constructor
     *
     * @param sliceName - slice name
     * @param workflowId - workflow id
     * @param region - chameleon region on which resources are allocated
     *
     */
    public ReqContext(String sliceName, String workflowId, String region, String authUrl) {
        this.sliceName = sliceName;
        this.workflowId = workflowId;
        activeOrFailedInstances = 0;
        instanceIdList = new LinkedList<>();
        leaseId = null;
        this.region = region;
        this.authUrl = authUrl;
        notificationSent = false;
        leaseEnd = null;
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
                for (String instanceId : instanceIdList) {
                    JSONObject id = new JSONObject();
                    id.put("id", instanceId);
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
                    instanceIdList.add((String) instanceId.get("id"));
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

            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();

            // Instantiate Jclouds based Openstack Controller object
            ComputeController computeController = new ComputeController(authUrl, user, password, userDomain, project);
            for(String instanceId : instanceIdList) {
                try {
                    computeController.destroyInstance(region, instanceId);
                }
                catch (Exception e) {
                    LOGGER.debug("Ignoring exception during destroy e=" + e);
                }
            }

            try {
                computeController.deleteKeyPair(region, sliceName);
            }
            catch (Exception e) {
                LOGGER.debug("Ignoring exception during destroy e=" + e);
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
     * @return name index
     * @throws Exception in case of error
     *
     */
    public int provisionNode(Map<String, Integer> flavorList, int nameIndex, String image,
                             String leaseEnd, String hostNamePrefix, String postBootScript,
                             Map<String, String> metaData, String networkId, String ip, String sgName) throws Exception {

        LOGGER.debug("IN flavorList=" + flavorList.toString() + " nameIndex=" + nameIndex + " image=" + image + " leaseEnd=" + leaseEnd
                + " hostNamePrefix=" + hostNamePrefix + " postBootScript=" + postBootScript + " metaData=" + metaData + " networkId=" + networkId
                + " ip=" + ip + " sgName=" + sgName);

        ComputeController computeController = null;
        try {

            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authUrl, user, password, userDomain, project);

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Jetstream, user);
            }

            // Extract image name
            if(image == null) {
                image = MobiusConfig.getInstance().getJetStreamDefaultImageName();
            }

            this.leaseEnd = new Date();
            if(leaseEnd != null) {
                this.leaseEnd = new Date(Long.parseLong(leaseEnd) * 1000);
            }
            else {
                this.leaseEnd.setTime(this.leaseEnd.getTime() + 86400000);
            }

            for(Map.Entry<String, Integer> entry : flavorList.entrySet()) {
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
                    LOGGER.debug("adding node=" + name + " with flavor=" + entry.getKey());

                    String instanceId = computeController.createInstance(region,
                            MobiusConfig.getInstance().getJetStreamUserSshKey(),
                            image,
                            entry.getKey(),
                            networkId,
                            null,
                            sliceName,
                            name,
                            postBootScript, metaData, ip, sgName);

                    if (instanceId == null) {
                        throw new MobiusException("Failed to create instance");
                    }
                    instanceIdList.add(instanceId);
                    ++nameIndex;
                }
            }
            return nameIndex;
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
            if(computeController != null) {
                computeController.close();
            }
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief function to generate JSONObject representing a single server
     *
     * @param server - server provisioned
     * @param ip - ip associated with server
     *
     * @return JSONObject representing server
     */
    private JSONObject nodeToJson(Server server, String ip){
        LOGGER.debug("IN server=" + server.toString() + " ip=" + ip);
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, server.getName());
        object.put(CloudContext.JsonKeyState, server.getStatus().toString());
        if(ip != null) {
            object.put(CloudContext.JsonKeyPublicIP, ip);
        }
        else {
            object.put(CloudContext.JsonKeyPublicIP, "");
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
        ComputeController computeController = null;
        JSONObject returnValue = new JSONObject();
        try {
            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();
            String floatingIpPool = MobiusConfig.getInstance().getJetStreamFloatingIpPool();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authUrl, user, password, userDomain, project);
            returnValue.put(CloudContext.JsonKeySlice, sliceName);
            JSONArray array = new JSONArray();

            for(String instanceId : instanceIdList) {
                try {
                    Server instance = computeController.getInstanceFromInstanceId(region, instanceId);
                    if(instance != null) {
                        String ip = computeController.getFloatingIpFromInstance(instance);
                        if (!hostNameSet.contains(instance.getName())) {
                            LOGGER.debug("Adding hostname: " + instance.getName());
                            hostNameSet.add(instance.getName());
                        }
                        LOGGER.debug("Floating ip for instance = " + ip);
                        if (instance.getStatus() == Server.Status.ACTIVE) {
                            if(ip == null) {
                                activeOrFailedInstances++;
                                FloatingIP floatingIP = computeController.allocateFloatingIp(region, floatingIpPool);

                                if (floatingIP == null) {
                                    throw new MobiusException("Failed to allocate floatingIP");
                                }

                                computeController.attachFloatingIp(region, instance, floatingIP);
                                ip = floatingIP.getIp();
                            }
                        } else if (instance.getStatus() == Server.Status.ERROR) {
                            activeOrFailedInstances++;
                        }
                        JSONObject object = nodeToJson(instance, ip);
                        array.add(object);
                    }
                    else {
                        LOGGER.error("Instance not found for InstanceId=" + instanceId);
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
            if(computeController != null) {
                computeController.close();
            }
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

    public int addStorage(StorageRequest request, int nameIndex) throws Exception {

        LOGGER.debug("IN request=" + request + " nameIndex=" + nameIndex);

        ComputeController computeController = null;
        try {

            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authUrl, user, password, userDomain, project);

            String name = workflowId + "-" + CloudContext.StorageNameSuffix + nameIndex;
            name = name.toLowerCase();

            Server server = computeController.getInstanceFromInstanceName(region, request.getTarget());

            String volumeId = computeController.addVolumeToServer(region, server.getId(), CloudContext.StorageDeviceName, request.getMountPoint(), name, request.getSize(), password);

            if (volumeId == null) {
                throw new MobiusException("Failed to add volume");
            }

            ++nameIndex;
            return nameIndex;
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to add storage e=" + e.getMessage());
        }
        finally {
            if(computeController != null) {
                computeController.close();
            }
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("OUT");
        }
    }
    public void deleteStorage(StorageRequest request) throws Exception {

        LOGGER.debug("IN request" + request);

        ComputeController computeController = null;
        try {

            String user = MobiusConfig.getInstance().getJetStreamUser();
            String password = MobiusConfig.getInstance().getJetStreamUserPassword();
            String userDomain = MobiusConfig.getInstance().getJetStreamUserDomain();
            String project = MobiusConfig.getInstance().getJetStreamProject();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authUrl, user, password, userDomain, project);

            Server server = computeController.getInstanceFromInstanceName(region, request.getTarget());

            computeController.deleteVolumesFromServer(region, server.getId());
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request e=" + e.getMessage());
        }
        finally {
            if(computeController != null) {
                computeController.close();
            }
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("OUT");
        }
    }
}

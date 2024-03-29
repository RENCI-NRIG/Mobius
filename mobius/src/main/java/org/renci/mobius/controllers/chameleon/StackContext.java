package org.renci.mobius.controllers.chameleon;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.ComputeController;
import org.renci.controllers.os.NetworkController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.ComputeResponse;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.sdx.SdxClient;
import org.renci.mobius.controllers.utils.OsSsoAuth;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.SdxPrefix;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * @brief class representing resources associated with a reservation; it represents all resources associated with
 *        a single mobius request
 * @author kthare10
 */
public class StackContext implements AutoCloseable{
    private static final Logger LOGGER = LogManager.getLogger( StackContext.class.getName() );
    public static final String RegionUC = "CHI@UC";
    public static final String RegionTACC = "CHI@TACC";
    public static final String RegionKVM = "KVM@TACC";
    public static final String RegionEDGE = "CHI@Edge";

    private String sliceName;
    private String workflowId;
    private int activeOrFailedInstances;
    private List<String> instanceIdList;
    private String leaseId;
    private String floatingIp;
    private String region;
    private boolean notificationSent;
    private ComputeController computeController;
    private NetworkController networkController;
    private OsContainerApi api;

    private final static String postBootScriptRequiredForComet = "#!/bin/bash\n" +
            "echo 'begin installing neuca'\n" +
            "pip install -U boto\n" +
            "pip install python-daemon==2.1.2\n" +
            "git clone  https://github.com/RENCI-NRIG/neuca-guest-tools /root/neuca-guest-tools\n" +
            "cd /root/neuca-guest-tools/neuca-py/\n" +
            "python setup.py  install\n" +
            "python /usr/bin/neucad start -c\n" +
            "cd /root\n" +
            "echo 'neuca install complete'\n";

    public final static String postBootScriptRequiredForStorage = "#!/bin/bash\n" +
            "WORKING_DIR=/root\n" +
            "{\n" +
            "echo 'begin installing neuca'\n" +
            "pip install -U boto\n" +
            "pip install python-daemon==2.1.2\n" +
            "git clone  https://github.com/RENCI-NRIG/neuca-guest-tools /root/neuca-guest-tools\n" +
            "cd /root/neuca-guest-tools/neuca-py/\n" +
            "python setup.py  install\n" +
            "python /usr/bin/neucad start -c\n" +
            "cd /root\n" +
            "echo 'neuca install complete'\n" +
            "#install some tools \n" +
            "echo nameserver 8.8.8.8 >> /etc/resolv.conf\n" +
            "yum install -y vim mlocate xfsprogs\n" +
            "\n" +
            "#Clean up node\n" +
            "ROOT_DISK=`mount -l | grep img-rootfs | awk '{ print $1 }'`\n" +
            "for i in `ls /dev/md*`; do\n" +
            "  #need to mdadm --stop /dev/md*\n" +
            "  echo removing $i\n" +
            "  dd if=/dev/zero of=$i bs=512 count=1 conv=notrunc \n" +
            "  wipefs -a $i\n" +
            "  mdadm --stop $i\n" +
            "  mdadm --remove $i\n" +
            "done\n" +
            "\n" +
            "for i in `ls /dev/sd*`; do\n" +
            "    echo cleaning $i\n" +
            "    [[ ! -b \"$i\" ]] && echo $i is not a block device... removing && rm -rf $i && continue\n" +
            "    [[ ! $ROOT_DISK =~ $i ]] && echo Destroying $i && dd if=/dev/zero of=$i bs=512 count=1 conv=notrunc && sleep 10 && wipefs -a $i\n" +
            "done\n" +
            "\n" +
            " \n" +
            "#Find all extra disks\n" +
            "count=0\n" +
            "devs=' '\n" +
            "for i in `ls /dev/sd*`; do\n" +
            "    #echo $i\n" +
            "    [[ ! $ROOT_DISK =~ $i ]] && echo Adding $i && (( ++count )) && devs=${devs}' '${i}\n" +
            "done\n" +
            "\n" +
            "echo ROOT_DISK $ROOT_DISK\n" +
            "echo count: $count\n" +
            "echo devs:  $devs\n" +
            "\n" +
            "#Make software RAID\n" +
            "yes | mdadm --create --verbose /dev/md0 --level=0 --raid-devices=$count $devs\n" +
            "wipefs -a /dev/md0\n" +
            "\n" +
            "echo Create FS\n" +
            "mkfs.xfs -i size=512 /dev/md0\n" +
            "mkdir -p /bricks/brick1\n" +
            "\n" +
            "echo /dev/md0 /bricks/brick1 xfs defaults 1 2 >> /etc/fstab\n" +
            "mount -a\n" +
            "if [[ $? != 0 ]]; then echo mkfs failed. bailing out; exit 1; fi\n" +
            "\n" +
            "mkdir /bricks/brick1/gv0\n" +
            "\n" +
            "echo Install GlusterFS\n" +
            "yum install -y centos-release-gluster\n" +
            "yum remove -y userspace-rcu\n" +
            "yum install -y glusterfs-server\n" +
            "\n" +
            "\n" +
            "\n" +
            "echo Start glusterfs\n" +
            "systemctl enable glusterd\n" +
            "#ln -s '/usr/lib/systemd/system/glusterd.service' '/etc/systemd/system/multi-user.target.wants/glusterd.service'\n" +
            "systemctl start glusterd\n" +
            "systemctl status glusterd\n" +
            "} > ${WORKING_DIR}/boot.log 2>&1";
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final static TimeZone utc = TimeZone.getTimeZone("UTC");

    /*
     * @brief constructor
     *
     * @param sliceName - slice name
     * @param workflowId - workflow id
     * @param region - chameleon region on which resources are allocated
     *
     */
    public StackContext(String sliceName, String workflowId, String region) throws Exception {
        this.sliceName = sliceName;
        this.workflowId = workflowId;
        activeOrFailedInstances = 0;
        instanceIdList = new LinkedList<>();
        leaseId = null;
        floatingIp = null;
        this.region = region;
        notificationSent = false;

        String user = MobiusConfig.getInstance().getChameleonUser();
        String password = MobiusConfig.getInstance().getChameleonUserPassword();
        String oidcPassword = MobiusConfig.getInstance().getChameleonUserPasswordOidc();
        String authurl = MobiusConfig.getInstance().getChameleonAuthUrl(region);
        String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
        String project = MobiusConfig.getInstance().getChameleonProject();
        String projectId = MobiusConfig.getInstance().getChameleonProjectId(region);
        String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

        String accessEndPoint = MobiusConfig.getInstance().getChameleonAccessTokenEndpoint();
        String federatedIdProvider = MobiusConfig.getInstance().getChameleonFederatedIdentityProvider(region);
        String clientId = MobiusConfig.getInstance().getChameleonClientId(region);
        String clientSecret = MobiusConfig.getInstance().getChameleonClientSecret();
        String scope = MobiusConfig.getInstance().getChameleonAccessEndpointScope();

        LOGGER.debug("accessEndPoint= " + accessEndPoint);
        LOGGER.debug("federatedIdProvider= " + federatedIdProvider);
        LOGGER.debug("clientId= " + clientId);
        LOGGER.debug("clientSecret= " + clientSecret);
        LOGGER.debug("scope= " + scope);
        LOGGER.debug("projectId= " + projectId);
        LOGGER.debug("password= " + password);
        LOGGER.debug("oidcPassword= " + oidcPassword);

        if(accessEndPoint != null && !accessEndPoint.isEmpty() &&
                federatedIdProvider != null && !federatedIdProvider.isEmpty() &&
                clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty() &&
                scope != null && !scope.isEmpty() && projectId != null && !projectId.isEmpty()) {
            OsSsoAuth ssoAuth = new OsSsoAuth(accessEndPoint, federatedIdProvider, clientId, clientSecret,
                    user, oidcPassword, scope);
            String federatedToken = ssoAuth.federatedToken();
            LOGGER.debug("federatedToken= " + federatedToken);
            computeController = new ComputeController(authurl, federatedToken, userDomain, projectId, true);
            networkController = new NetworkController(authurl, federatedToken, userDomain, projectId, true);
            api = new OsContainerApi(authurl, federatedToken, userDomain, projectId, projectDomain);
        }
        else {
            // Instantiate Jclouds based Openstack Controller object
            LOGGER.debug("authurl= " + authurl);
            LOGGER.debug("user= " + user);
            LOGGER.debug("password= " + password);
            LOGGER.debug("userDomain= " + userDomain);
            LOGGER.debug("project= " + project);
            computeController = new ComputeController(authurl, user, password, userDomain, project);
            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            api = new OsContainerApi(authurl, user, password, userDomain, project, projectDomain);
        }
    }
    /*
     * @brief close computeController
     */
    public void close() {
        computeController.close();
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
     * @brief return region
     * @return retrun region
     */
    public String getRegion() {
        return region;
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
            if(floatingIp != null) {
                retVal.put("floatingIp", floatingIp);
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
            if (object.get("floatingIp") != null) {
                floatingIp = (String) object.get("floatingIp");
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

            for(String instanceId : instanceIdList) {
                try {
                    if (region.compareToIgnoreCase(StackContext.RegionEDGE) == 0) {
                        api.delete(region, instanceId);
                    }
                    else {
                        computeController.destroyInstance(region, instanceId);
                    }
                }
                catch (Exception e) {
                    LOGGER.debug("Ignoring exception during destroy e=" + e);
                }
            }

            try {
                if (region.compareToIgnoreCase(StackContext.RegionEDGE) != 0) {
                    computeController.deleteKeyPair(region, sliceName);
                }
            }
            catch (Exception e) {
                LOGGER.debug("Ignoring exception during destroy e=" + e);
            }

            api.deleteLease(region, leaseId);
        }
        catch (Exception e){
            LOGGER.debug("Exception occurred while deleting slice " + sliceName);
        }
        LOGGER.debug("Instance destruction taking plance =============================");
        LOGGER.debug("OUT");
    }

    String buildLease(String sliceName, String leaseEnd, Map<String, Integer> nodeTypeCountMap) {
        String reservationRequest = null;
        sdf.setTimeZone(utc);
        Date endTime = new Date();
        if(leaseEnd != null) {
            endTime = new Date(Long.parseLong(leaseEnd) * 1000);
        }
        else {
            endTime.setTime(endTime.getTime() + 86400000);
        }

        Date now = new Date();
        now.setTime(now.getTime() + 60000);

        if (region.compareToIgnoreCase(StackContext.RegionEDGE) != 0) {
            reservationRequest = api.buildComputeLeaseRequest(sliceName, sdf.format(now), sdf.format(endTime), nodeTypeCountMap);
        }
        else {
            reservationRequest = api.buildContainerLeaseRequest(sliceName, sdf.format(now), sdf.format(endTime), nodeTypeCountMap);
        }
        return reservationRequest;
    }

    private ComputeResponse provisionBareMetal(Map<String, Integer> flavorList, String hostNamePrefix,
                                               String postBootScript, String leaseEnd, String image, int nameIndex,
                                               Map<String, String> metaData, String networkId, String ip, String sgName) throws Exception {
        ComputeResponse response = new ComputeResponse(0, 0);

        String reservationRequest = buildLease(sliceName, leaseEnd, flavorList);

        if (reservationRequest == null) {
            throw new MobiusException("Failed to construct reservation request");
        }

        Pair<String, Map<String, Integer>> result = api.createLease(region, sliceName, reservationRequest,
                600);

        if (result == null || result.getFirst() == null || result.getSecond() == null) {
            throw new MobiusException("Failed to request lease");
        }

        leaseId = result.getFirst();
        Map<String, Integer> reservationIds = result.getSecond();


        LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        LOGGER.debug("Reservation Id used for instance creation=" + reservationIds);
        LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        for (Map.Entry<String, Integer> entry : reservationIds.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                String name = workflowId + "-";
                if (hostNamePrefix != null) {
                    name = workflowId + hostNamePrefix + nameIndex;
                } else {
                    name = name + CloudContext.NodeName + nameIndex;
                }
                name = name.toLowerCase();
                LOGGER.debug("adding node=" + name);
                if (!image.contains("Ubuntu")) {
                    response.addHost(name + ".novalocal", null);
                }
                else {
                    response.addHost(name, null);
                }

                Map<String, String> meta = null;
                if (metaData != null) {
                    metaData.put("reservation_id", name);
                    meta = metaData;
                }

                if (postBootScript == null) {
                    postBootScript = postBootScriptRequiredForComet;
                }

                String instanceId = computeController.createInstance(region,
                        MobiusConfig.getInstance().getDefaultChameleonUserSshKey(),
                        image,
                        MobiusConfig.getInstance().getChameleonDefaultFlavorName(),
                        networkId,
                        entry.getKey(),
                        sliceName,
                        name,
                        postBootScript, meta, ip, sgName);

                if (instanceId == null) {
                    throw new MobiusException("Failed to create instance");
                }
                instanceIdList.add(instanceId);
                ++nameIndex;
            }
        }
        response.setNodeCount(nameIndex);
        return response;
    }

    private ComputeResponse provisionEdge(Map<String, Integer> flavorList, int nameIndex,
                                          String networkId, ComputeRequest request) throws Exception {
        ComputeResponse response = new ComputeResponse(0, 0);
        Map<String, Integer> reservationIds = null;

        if(request.getReservationId() == null) {
            String reservationRequest = buildLease(sliceName, request.getLeaseEnd(), flavorList);

            if (reservationRequest == null) {
                throw new MobiusException("Failed to construct reservation request");
            }

            Pair<String, Map<String, Integer>> result = api.createLease(region, sliceName, reservationRequest,
                    600);

            if (result == null || result.getFirst() == null || result.getSecond() == null) {
                throw new MobiusException("Failed to request lease");
            }

            leaseId = result.getFirst();
            reservationIds = result.getSecond();
        }
        else {
            reservationIds = new HashMap<>();
            reservationIds.put(request.getReservationId(), 1);
        }


        LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        LOGGER.debug("Reservation Id used for instance creation=" + reservationIds);
        LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        for (Map.Entry<String, Integer> entry : reservationIds.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                String name = workflowId + "-";
                if (request.getHostNamePrefix() != null) {
                    name = workflowId + request.getHostNamePrefix() + nameIndex;
                } else {
                    name = name + CloudContext.NodeName + nameIndex;
                }
                name = name.toLowerCase();
                LOGGER.debug("adding node=" + name);
                if (!request.getImageName().contains("Ubuntu")) {
                    response.addHost(name + ".novalocal", null);
                }
                else {
                    response.addHost(name, null);
                }
                Map<String, Object> result = api.create(region, name, request.getImageName(), null,
                        request.getEnvironment(), null, null, networkId, entry.getKey(),
                        request.getPostBootScript(), request.getWorkDirectory(), request.getLabels(), request.getMounts());

                String instanceId = (String) result.get("uuid");
                instanceIdList.add(instanceId);
                String status = (String) result.get("status");
                if (status.compareToIgnoreCase("error") == 0) {
                    status = (String) result.get("status_reason");
                    LOGGER.error(String.format("Failed to create container: %s", status));
                }

                ++nameIndex;
            }
        }
        response.setNodeCount(nameIndex);
        return response;
    }


    private ComputeResponse provisionKvm(Map<String, Integer> flavorList, String hostNamePrefix, String postBootScript,
                              String image, int nameIndex, Map<String, String> metaData, String networkId,
                              String ip, String sgName) throws Exception {
        ComputeResponse response = new ComputeResponse(0, 0);
        for(Map.Entry<String, Integer> entry : flavorList.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                String name = workflowId + "-";
                if (hostNamePrefix != null) {
                    name = workflowId + hostNamePrefix + nameIndex;
                } else {
                    name = name + CloudContext.NodeName + nameIndex;
                }
                name = name.toLowerCase();
                if (!image.contains("Ubuntu")) {
                    response.addHost(name + ".novalocal", null);
                }
                else {
                    response.addHost(name, null);
                }
                LOGGER.debug("adding node=" + name + " with flavor=" + entry.getKey());

                String instanceId = computeController.createInstance(region,
                        MobiusConfig.getInstance().getDefaultChameleonUserSshKey(),
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
        response.setNodeCount(nameIndex);
        return response;
    }

    /*
     * @brief function to provision a node on chameleon; if no post boot script is specified
     *        default post boot script to install neuca tool is passed; creates a lease; waits for lease
     *        to be active and then provisions the node. if lease does not become active in a timeout,
     *        request is treated as a failure
     *
     * @param flavorList - map of <flavorname, number of nodes for the flavor> to be instantiated
     * @param nameIndex - number representing index to be added to instance name
     * @param metaData - meta data
     * @param networkId - network id to which instance is connected
     * @param sgName - sgName
     *
     * @return name index
     * @throws Exception in case of error
     */
    public ComputeResponse provisionNode(Map<String, Integer> flavorList, int nameIndex, ComputeRequest request,
                                         Map<String, String> metaData, String networkId, String sgName) throws Exception {

        LOGGER.debug("IN flavorList=" + flavorList.toString() + " nameIndex=" + nameIndex
                + " metaData=" + metaData.toString() + " networkId=" + networkId +" sgName=" + sgName);

        String image = request.getImageName();
        String leaseEnd = request.getLeaseEnd();

        try {

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Chameleon,
                        MobiusConfig.getInstance().getChameleonUser());
            }

            // Extract image name
            if(request.getImageName() == null) {
                image = MobiusConfig.getInstance().getDefaultChameleonImageName();
            }

            sdf.setTimeZone(utc);
            Date endTime = new Date();
            if(leaseEnd != null) {
                endTime = new Date(Long.parseLong(leaseEnd) * 1000);
            }
            else {
                endTime.setTime(endTime.getTime() + 86400000);
            }

            Date now = new Date();
            now.setTime(now.getTime() + 60000);

            ComputeResponse response = new ComputeResponse(0, 0);
            if (region.compareToIgnoreCase(StackContext.RegionKVM) == 0)
                response = provisionKvm(flavorList, request.getHostNamePrefix(), request.getPostBootScript(),
                        image, nameIndex, metaData, networkId, request.getIpAddress(),  sgName);
            else if (region.compareToIgnoreCase(StackContext.RegionEDGE) == 0)
                    response = provisionEdge(flavorList, nameIndex, networkId, request);
            else
                    response = provisionBareMetal(flavorList, request.getHostNamePrefix(),
                            request.getPostBootScript(), leaseEnd, image, nameIndex, metaData, networkId,
                            request.getIpAddress(), sgName);
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
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief function to provision a node on chameleon; if no post boot script is specified
     *        default post boot script to install neuca tool is passed; creates a lease; waits for lease
     *        to be active and then provisions the node. if lease does not become active in a timeout,
     *        request is treated as a failure
     *
     * @param flavorList - map of <flavorname, number of nodes for the flavor> to be instantiated
     * @param nameIndex - number representing index to be added to instance name
     * @param metaData - meta data
     * @param networkId - network id to which instance is connected
     * @param sgName - sgName
     *
     * @return name index
     * @throws Exception in case of error
     */
    public ComputeResponse provisionStorage(Map<String, Integer> flavorList, int nameIndex, StorageRequest request,
                                         Map<String, String> metaData, String networkId, String sgName) throws Exception {

        LOGGER.debug("IN flavorList=" + flavorList.toString() + " nameIndex=" + nameIndex
                + " metaData=" + metaData.toString() + " networkId=" + networkId +" sgName=" + sgName);

        String image = MobiusConfig.getInstance().getDefaultChameleonImageName();
        String leaseEnd = request.getLeaseEnd();
        String prefix = request.getTarget() + CloudContext.StorageNameSuffix;

        try {

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Chameleon,
                        MobiusConfig.getInstance().getChameleonUser());
            }

            sdf.setTimeZone(utc);
            Date endTime = new Date();
            if(leaseEnd != null) {
                endTime = new Date(Long.parseLong(leaseEnd) * 1000);
            }
            else {
                endTime.setTime(endTime.getTime() + 86400000);
            }

            Date now = new Date();
            now.setTime(now.getTime() + 60000);

            ComputeResponse response = provisionBareMetal(flavorList, prefix,
                    StackContext.postBootScriptRequiredForStorage, leaseEnd, image, nameIndex, metaData, networkId,
                            null, sgName);
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
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief function to generate JSONObject representing a single server
     *
     * @param server - server provisioned
     * @param ip - ip associated with server
     * @param fixedIPs - fixedIPs associated with server
     * @return JSONObject representing server
     */
    private JSONObject nodeToJson(Server server, String ip, List<String> fixedIPs){
        LOGGER.debug("IN server=" + server.toString() + " ip=" + ip + " fixedIPs=" + fixedIPs);
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, server.getName());
        object.put(CloudContext.JsonKeyState, server.getStatus().toString());
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
        LOGGER.debug("OUT object=" + object.toString());
        return object;
    }

    /*
     * @brief function to generate JSONObject representing a single server
     *
     * @param server - server provisioned
     * @param ip - ip associated with server
     * @param fixedIPs - fixedIPs associated with server
     * @return JSONObject representing server
     */
    private JSONObject nodeToJson(Map<String, Object> container, String ip){
        LOGGER.debug("IN container=" + container.toString() + " ip=" + ip );
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, container.get("name"));
        object.put(CloudContext.JsonKeyState, container.get("status"));
        if(ip != null) {
            object.put(CloudContext.JsonKeyPublicIP, ip);
        }
        else {
            object.put(CloudContext.JsonKeyPublicIP, "");
        }
        List<String> fixedIPs = new LinkedList<>();
        Map<String, ArrayList<Object>> addresses = (Map<String, ArrayList<Object>>) container.get("addresses");
        for (Object add_list_object : addresses.values()) {
            ArrayList<Object> add_list = (ArrayList<Object>) add_list_object;
            for (Object add_details: add_list) {
                Map<String, String> details = (Map<String, String>) add_details;
                String fixedIp = details.get("addr");
                if (fixedIp != null) {
                    fixedIPs.add(fixedIp);
                }
            }
        }
        if(fixedIPs != null) {
            int index = 1;
            for(String i: fixedIPs) {
                object.put(CloudContext.JsonKeyIP + Integer.toString(index), i);
            }
        }
        LOGGER.debug("OUT object=" + object.toString());
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
        LOGGER.debug("IN hostNameSet=" + hostNameSet.toString());
        JSONObject returnValue = new JSONObject();
        try {
            String floatingIpPool = MobiusConfig.getInstance().getChameleonFloatingIpPool();

            // Instantiate Jclouds based Openstack Controller object
            returnValue.put(CloudContext.JsonKeySlice, sliceName);
            JSONArray array = new JSONArray();

            for(String instanceId : instanceIdList) {
                try {
                    if (region.compareToIgnoreCase(StackContext.RegionEDGE) != 0) {
                        Server instance = computeController.getInstanceFromInstanceId(region, instanceId);
                        if (instance != null) {
                            String ip = computeController.getFloatingIpFromInstance(instance);
                            if (!hostNameSet.contains(instance.getName())) {
                                LOGGER.debug("Adding hostname: " + instance.getName());
                                hostNameSet.add(instance.getName());
                            }
                            LOGGER.debug("Floating ip for instance = " + ip);
                            if (instance.getStatus() == Server.Status.ACTIVE) {
                                if (ip == null) {
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
                            List<String> fixedIPs = computeController.getFixedIpFromInstance(instance);
                            ;
                            JSONObject object = nodeToJson(instance, ip, fixedIPs);
                            array.add(object);
                        } else {
                            LOGGER.error("Instance not found for InstanceId=" + instanceId);
                        }
                    }
                    else {
                        Map<String, Object> container = api.get(region, instanceId);
                        if (container != null) {
                            String status = (String) container.get("status");
                            if(status.compareToIgnoreCase("Running") == 0 && floatingIp == null) {
                                LOGGER.debug("Container is running with no floating ipd");
                                String fixedIp = null, port = null;
                                Map<String, ArrayList<Object>> addresses = (Map<String, ArrayList<Object>>) container.get("addresses");
                                LOGGER.debug(addresses);
                                for (Object add_list_object : addresses.values()) {
                                    ArrayList<Object> add_list = (ArrayList<Object>) add_list_object;
                                    LOGGER.debug(add_list);
                                    for (Object add_details: add_list) {
                                        Map<String, String> details = (Map<String, String>) add_details;
                                        fixedIp = details.get("addr");
                                        port = details.get("port");
                                        if (fixedIp != null && port != null) {
                                            break;
                                        }
                                    }
                                }
                                LOGGER.debug("attach FIP");
                                LOGGER.debug(floatingIpPool);
                                LOGGER.debug(port);
                                LOGGER.debug(fixedIp);
                                floatingIp = networkController.attachFip(region, floatingIpPool, port, fixedIp);
                                LOGGER.debug("attach FIP complete");
                            }
                            JSONObject object = nodeToJson(container, floatingIp);
                            array.add(object);
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Exception occurred while checking status e=" + e);
                    e.printStackTrace();
                }
            }

            returnValue.put(CloudContext.JsonKeyNodes, array);
        }
        catch (Exception e){
            LOGGER.error("Exception occurred while getting status of slice " + sliceName);
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("OUT returnValue=" + returnValue);
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
        LOGGER.debug("IN hostNameSet=" + hostNameSet.toString());

        JSONObject object = null;
        try {
            object = status(hostNameSet);
        }
        catch (Exception e){
            LOGGER.error("Exception occurred while performing periodic updates to slice " + sliceName);
        }
        LOGGER.debug("OUT object=" + object.toString());
        return object;
    }
    public void renew(String leaseEnd) throws Exception{
        LOGGER.debug("IN leaseEnd=" + leaseEnd);

        try {
            sdf.setTimeZone(utc);
            Date endTime = new Date();
            if(leaseEnd != null) {
                endTime = new Date(Long.parseLong(leaseEnd) * 1000);
            }
            else {
                endTime.setTime(endTime.getTime() + 86400000);
            }

            api.updateLease(region, leaseId, sdf.format(endTime));
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server compute request e=" + e.getMessage());
        }
        finally {
            LOGGER.debug("OUT");
        }

    }

    public Server getInstance(String hostname) {
        LOGGER.debug("IN hostname=" + hostname);
        Server instance = null;
        try {
            for(String instanceId : instanceIdList) {
                instance = computeController.getInstanceFromInstanceId(region, instanceId);
                if(instance != null && instance.getName().equalsIgnoreCase(hostname)) {
                    break;
                }
            }

            if(instance == null) {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "Instance with " + hostname + " not found");
            }

            if(instance.getStatus() != Server.Status.ACTIVE) {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "Instance with " + hostname + " not active");
            }
        }
        catch (Exception e){
            LOGGER.error("Exception occurred while getting status of slice " + sliceName);
            LOGGER.error("Ex= " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("OUT returnValue=" + instance);
        }
        return instance;
    }
    /*
     * @brief function to stitch to sdx and advertise a prefix for add operation and unstitch in case of delete
     *
     * @param hostName - hostName
     * @param vlan - vlan
     * @param subnet - subnet
     * @param localSubnet - localSubnet
     * @param action - action
     * @param destSite - destSite
     * @param sdxStitchPortInterfaceIP - sdxStitchPortInterfaceIP (used only for chameleon)
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequestSetupStitchingAndRoute(String hostname, String vlan, String subnet,
                                                            String localSubnet,
                                                            NetworkRequest.ActionEnum action, String destSite,
                                                            String sdxStitchPortInterfaceIP) throws Exception{
        LOGGER.debug("IN");

        if (region.compareToIgnoreCase(StackContext.RegionEDGE) == 0) {
            throw new MobiusException("Stitching not supported for Containers");
        }

        try {
            SdxClient sdxClient = new SdxClient(MobiusConfig.getInstance().getMobiusSdxUrl());
            Server instance = getInstance(hostname);

            switch (action) {
                case ADD:
                {
                    List<String> fixedIPs = computeController.getFixedIpFromInstance(instance);
                    if(fixedIPs.size() == 0) {
                        throw new MobiusException("No fixed IPs associated with " + hostname);
                    }
                    String stitchPort = null;
                    if(region.equalsIgnoreCase(RegionUC)) {
                        stitchPort = MobiusConfig.getInstance().getChameleonUCStitchPort();
                    }
                    else if(region.equalsIgnoreCase(RegionTACC)) {
                        stitchPort = MobiusConfig.getInstance().getChameleonTACCStitchPort();
                    }
                    sdxClient.stitchChameleon(stitchPort, vlan, fixedIPs.get(0), sdxStitchPortInterfaceIP,
                            destSite, sliceName);
                    sdxClient.prefix(sliceName, fixedIPs.get(0), subnet);
                    if(localSubnet != null) {
                        sdxClient.prefix(sliceName, fixedIPs.get(0), localSubnet);
                    }
                }
                break;
                case DELETE: {
                    // unstitch
                    sdxClient.unstitch(sliceName, sliceName);
                    break;
                }
                default:
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "unsupported network operation");
            }
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server stitch request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("OUT");
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
    public void processNetworkRequestLink(String hostname, String subnet1, String subnet2,
                                          String bandwidth, String destinationIP,
                                          String sdxStitchPortInterfaceIP) throws Exception{
        LOGGER.debug("IN hostname=" + hostname + " subnet1=" + subnet1 + " subnet2=" + subnet2 + " bandwidth=" + bandwidth);
        if (region.compareToIgnoreCase(StackContext.RegionEDGE) == 0) {
            throw new MobiusException("Stitching not supported for Containers");
        }
        try {

            Server node = getInstance(hostname);
            String ip = null;
            if (node != null) {
                ip = computeController.getFloatingIpFromInstance(node);
            } else {
                throw new MobiusException("Unable to find the node in slice");
            }


            // hack needed to remove .pub from filename as SDX code expects to not have file extension
            SdxClient sdxClient = new SdxClient(MobiusConfig.getInstance().getMobiusSdxUrl() );

            sdxClient.connect(subnet1, subnet2, bandwidth);
            /*
            sdxStitchPortInterfaceIP = sdxStitchPortInterfaceIP.split("/")[0];
            String firstThree = destinationIP.replaceFirst("\\d+$", "");
            String command = String.format("sudo ip route add %s0/24 via %s", firstThree, sdxStitchPortInterfaceIP);
            RemoteCommand remoteCommand = new RemoteCommand("cc", MobiusConfig.getInstance().getDefaultExogeniUserSshPrivateKey());
            remoteCommand.runCmdByIP(command, ip,false);
            */
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server connect request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    public void processSdxPrefix(SdxPrefix request) throws Exception {
        LOGGER.debug("IN");
        if (region.compareToIgnoreCase(StackContext.RegionEDGE) == 0) {
            throw new MobiusException("Stitching not supported for Containers");
        }

        try {
            SdxClient sdxClient = new SdxClient(MobiusConfig.getInstance().getMobiusSdxUrl());
            sdxClient.prefix(sliceName, request.getGatewayIP(), request.getSourceSubnet());
            if (request.getDestinationSubnet() != null) {
                sdxClient.connect(request.getSourceSubnet(), request.getDestinationSubnet(), request.getBandwidth());
            }
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw new MobiusException("Failed to server stitch request = " + e.getLocalizedMessage());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

}

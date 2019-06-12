package org.renci.mobius.controllers.chameleon;


import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.ComputeController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.springframework.data.util.Pair;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * @brief class representing resources associated with a reservation; it represents all resources associated with
 *        a single mobius request
 * @author kthare10
 */
public class StackContext {
    private static final Logger LOGGER = Logger.getLogger( StackContext.class.getName() );

    private String sliceName;
    private String workflowId;
    private int activeOrFailedInstances;
    private List<String> instanceIdList;
    private String leaseId;
    private String region;
    private boolean notificationSent;

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
    public StackContext(String sliceName, String workflowId, String region) {
        this.sliceName = sliceName;
        this.workflowId = workflowId;
        activeOrFailedInstances = 0;
        instanceIdList = new LinkedList<>();
        leaseId = null;
        this.region = region;
        notificationSent = false;
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
        LOGGER.debug("stop: IN");
        LOGGER.debug("Instance destruction taking plance =============================");

        try {
            LOGGER.debug("Successfully deleted slice " + sliceName);

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

            // Instantiate Jclouds based Openstack Controller object
            ComputeController computeController = new ComputeController(authurl, user, password, userDomain, project);

            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            OsReservationApi api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);

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

            api.deleteLease(region, leaseId);
        }
        catch (Exception e){
            LOGGER.debug("Exception occured while deleting slice " + sliceName);
        }
        LOGGER.debug("Instance destruction taking plance =============================");
        LOGGER.debug("stop: OUT");
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
     *
     * @return name index
     * @throws Exception in case of error
     */
    public int provisionNode(Map<String, Integer> flavorList, int nameIndex, String image,
                             String leaseEnd, String hostNamePrefix, String postBootScript,
                             Map<String, String> metaData, String networkId, String ip) throws Exception {

        LOGGER.debug("provisionNode: IN");

        ComputeController computeController = null;
        OsReservationApi api = null;
        try {

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authurl, user, password, userDomain, project);

            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Chameleon, user);
            }

            // Extract image name
            if(image == null) {
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

            String reservationRequest = api.buildComputeLeaseRequest(sliceName, sdf.format(now),
                    sdf.format(endTime), flavorList);

            if(reservationRequest == null) {
                throw new MobiusException("Failed to construct reservation request");
            }

            Pair<String, Map<String, Integer>> result = api.createLease(region, sliceName,
                    reservationRequest, 300);

            if(result == null || result.getFirst() == null || result.getSecond() == null) {
                throw new MobiusException("Failed to request lease");
            }

            leaseId = result.getFirst();
            Map<String, Integer> reservationIds = result.getSecond();

            LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            LOGGER.debug("Reservation Id used for instance creation=" + reservationIds);
            LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

            for(Map.Entry<String, Integer> entry : reservationIds.entrySet()) {
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
                    LOGGER.debug("adding node=" + name);

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
                            postBootScript, meta, ip, null);

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
            LOGGER.debug("provisionNode: OUT");
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
        LOGGER.debug("nodeToJson: IN");
        JSONObject object = new JSONObject();
        object.put(CloudContext.JsonKeyName, server.getName());
        object.put(CloudContext.JsonKeyState, server.getStatus().toString());
        if(ip != null) {
            object.put(CloudContext.JsonKeyPublicIP, ip);
        }
        else {
            object.put(CloudContext.JsonKeyPublicIP, "");
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
    public JSONObject status(Set<String> hostNameSet) {
        LOGGER.debug("status: IN");
        ComputeController computeController = null;
        JSONObject returnValue = new JSONObject();
        try {
            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String floatingIpPool = MobiusConfig.getInstance().getChameleonFloatingIpPool();

            // Instantiate Jclouds based Openstack Controller object
            computeController = new ComputeController(authurl, user, password, userDomain, project);
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
            LOGGER.debug("status: OUT");
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
        LOGGER.debug("doPeriodic: IN");

        JSONObject object = null;
        try {
            object = status(hostNameSet);
        }
        catch (Exception e){
            LOGGER.error("Exception occured while performing periodic updates to slice " + sliceName);
        }
        LOGGER.debug("doPeriodic: OUT");
        return object;
    }
    public void renew(String leaseEnd) throws Exception{
        LOGGER.debug("renew: IN");

        try {

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            OsReservationApi api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);

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
            LOGGER.debug("renew: OUT");
        }

    }
}

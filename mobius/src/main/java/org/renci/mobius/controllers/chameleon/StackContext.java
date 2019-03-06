package org.renci.mobius.controllers.chameleon;


import org.apache.log4j.Logger;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.controllers.os.OpenstackController;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusConfig;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.*;

public class StackContext {
    private static final Logger LOGGER = Logger.getLogger( StackContext.class.getName() );

    private String sliceName;
    private int activeOrFailedInstances;
    private List<String> instanceIdList;
    private String leaseId;
    private String region;
    private boolean notificationSent;

    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    final TimeZone utc = TimeZone.getTimeZone("UTC");

    public StackContext(String sliceName) {
        this.sliceName = sliceName;
        activeOrFailedInstances = 0;
        instanceIdList = new LinkedList<>();
        leaseId = null;
        region = null;
        notificationSent = false;
    }

    public String getSliceName() {
        return sliceName;
    }

    public void setNotificationSent() { notificationSent = true; }

    public boolean canTriggerNotification() {
        if(!notificationSent && (activeOrFailedInstances > 0 || activeOrFailedInstances == instanceIdList.size())) {
            return true;
        }
        return false;
    }

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

    public void stop() {
        LOGGER.debug("stop: IN");

        try {
            LOGGER.debug("Successfully deleted slice " + sliceName);

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

            // Instantiate Jclouds based Openstack Controller object
            OpenstackController oscontroller = new OpenstackController(authurl, user, password, userDomain, project);

            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            OsReservationApi api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);

            for(String instanceId : instanceIdList) {
                try {
                    oscontroller.destroyInstance(region, instanceId);
                }
                catch (Exception e) {
                    LOGGER.debug("Ignoring exception during destroy e=" + e);
                }
            }

            try {
                oscontroller.deleteKeyPair(region, sliceName);
            }
            catch (Exception e) {
                LOGGER.debug("Ignoring exception during destroy e=" + e);
            }

            api.deleteLease(region, leaseId);
        }
        catch (Exception e){
            LOGGER.debug("Exception occured while deleting slice " + sliceName);
        }
        LOGGER.debug("stop: OUT");
    }

    public int processCompute(Map<String, Integer> flavorList, int nameIndex, ComputeRequest request) throws Exception {

        LOGGER.debug("processCompute: IN");

        try {

            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String projectDomain = MobiusConfig.getInstance().getChameleonProjectDomain();

            // Instantiate Jclouds based Openstack Controller object
            OpenstackController oscontroller = new OpenstackController(authurl, user, password, userDomain, project);

            // Instantiate Spring-framework based Rest API interface for openstack reservation apis not supported by
            // jclouds
            OsReservationApi api = new OsReservationApi(authurl, user, password, userDomain, project, projectDomain);

            // First compute request
            if (sliceName == null) {
                sliceName = CloudContext.generateSliceName(CloudContext.CloudType.Chameleon, user);
            }

            // Extract Region(Domain) from Site field
            String[] arrOfStr = request.getSite().split(":");
            if(arrOfStr.length < 2 || arrOfStr.length > 2) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Invalid Site name");
            }
            region = arrOfStr[1];
            LOGGER.debug("Site=" + request.getSite());
            LOGGER.debug("Region=" + region);

            String image = null;

            // Extract image name
            if(request.getImageName() != null) {
                image = request.getImageName();
            }
            else {
                image = MobiusConfig.getInstance().getDefaultChameleonImageName();
            }

            sdf.setTimeZone(utc);
            Date endTime = new Date(Long.parseLong(request.getLeaseEnd()) * 1000);

            // TODO effificently create lease; current implementation creates one lease per resource
            Date now = new Date();
            now.setTime(now.getTime() + 60000);

            Pair<String, Integer> reservationRequest = api.constructHostLeaseRequest(sliceName, sdf.format(now),
                    sdf.format(endTime), flavorList);

            if(reservationRequest == null) {
                throw new MobiusException("Failed to construct reservation request");
            }

            Pair<String, String> result = api.createComputeLease(region, sliceName,
                    reservationRequest.getFirst(), 120);

            if(result == null || result.getFirst() == null || result.getSecond() == null) {
                throw new MobiusException("Failed to request lease");
            }

            leaseId = result.getFirst();
            String reservationId = result.getSecond();

            LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            LOGGER.debug("Reservation Id used for instance creation=" + reservationId);
            LOGGER.debug("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

            for (int i =0; i< reservationRequest.getSecond(); ++i) {
                LOGGER.debug("adding node=" + nameIndex);
                String name = sliceName + "-" + CloudContext.NodeName + nameIndex;

                String instanceId = oscontroller.createInstance(region,
                        MobiusConfig.getInstance().getDefaultChameleonUserSshKey(),
                        image,
                        MobiusConfig.getInstance().getChameleonDefaultFlavorName(),
                        MobiusConfig.getInstance().getChameleonDefaultNetwork(),
                        reservationId,
                        sliceName,
                        name);

                if(instanceId == null) {
                    throw new MobiusException("Failed to create instance");
                }
                instanceIdList.add(instanceId);
                ++nameIndex;
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
            throw new MobiusException("Failed to server compute request");
        }
        finally {
            // TODO clean any allocated CPUs, keys, leases
            LOGGER.debug("processCompute: OUT");
        }
    }

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

    public JSONObject status(Set<String> hostNameSet) {
        LOGGER.debug("status: IN");
        JSONObject returnValue = new JSONObject();
        try {
            String user = MobiusConfig.getInstance().getChameleonUser();
            String password = MobiusConfig.getInstance().getChameleonUserPassword();
            String authurl = MobiusConfig.getInstance().getChameleonAuthUrl();
            String userDomain = MobiusConfig.getInstance().getChameleonUserDomain();
            String project = MobiusConfig.getInstance().getChameleonProject();
            String floatingIpPool = MobiusConfig.getInstance().getChameleonFloatingIpPool();

            // Instantiate Jclouds based Openstack Controller object
            OpenstackController oscontroller = new OpenstackController(authurl, user, password, userDomain, project);
            returnValue.put(CloudContext.JsonKeySlice, sliceName);
            JSONArray array = new JSONArray();

            for(String instanceId : instanceIdList) {
                try {
                    Server instance = oscontroller.getInstanceFromInstanceId(region, instanceId);
                    String ip = oscontroller.getFloatingIpFromInstance(instance);
                    LOGGER.debug("Floating ip for instance = " + ip);
                    if (instance.getStatus() == Server.Status.ACTIVE && ip == null) {
                        activeOrFailedInstances++;
                        FloatingIP floatingIP = oscontroller.allocateFloatingIp(region, floatingIpPool);

                        if (floatingIP == null) {
                            throw new MobiusException("Failed to allocate floatingIP");
                        }

                        oscontroller.attachFloatingIp(region, instance, floatingIP);
                        if(!hostNameSet.contains(instance.getName())) {
                            hostNameSet.add(instance.getName());
                        }

                        ip = floatingIP.getIp();
                    }
                    else if(instance.getStatus() == Server.Status.ERROR) {
                        activeOrFailedInstances++;
                    }
                    JSONObject object = nodeToJson(instance, ip);
                    array.add(object);
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
            LOGGER.debug("status: OUT");
        }
        return returnValue;
    }
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
}

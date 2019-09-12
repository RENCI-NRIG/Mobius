package org.renci.mobius.controllers;

import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * @brief class represents context for all resources on a specific cloud
 *
 * @author kthare10
 */
abstract public class CloudContext {
    public enum CloudType {
        Chameleon,
        Exogeni,
        Jetstream,
        OSG,
        Unknown
    }
    public static final String NetworkName = "cmnw";
    public static final String StorageNetworkName = "stnw";
    public static final String StorageNameSuffix = "storage";
    public static final String StorageDeviceName = "/dev/xvdd";
    public static final String NodeName = "Node";
    public static final String StitchPortName = "SP";
    public static final Integer AllowedDeltaTimeInMsFromCurrentTime = 300000; // 300 seconds i.e. 5 minutes
    public static final Long minimumTimeDifInMs = 86400000L; // 24 hours
    public static final String JsonKeySlice = "slice";
    public static final String JsonKeyNodes = "nodes";
    public static final String JsonKeyName = "name";
    public static final String JsonKeyState = "state";
    public static final String JsonKeyPublicIP = "publicIP";
    public static final String JsonKeyIP = "ip";
    public static final String JsonKeySite = "site";
    public static final String JsonKeySlices = "slices";
    public static final String JsonKeyVlan = "vlan";


    /*
     * @brief Random string generator for read and write token
     *
     * @return random string
     */
    public static String generateRandomString() {
        return RandomStringUtils.random( 10, true, true);
    }

    /*
     * @brief generate slice name give cloud type and user name
     *
     * @param type - cloud type
     * @param user - user name
     *
     * @return random string
     */
    public static String generateSliceName(CloudType type, String user) {
        return "Mobius-" + type.name() + "-" + user + "-" + generateRandomString();
    }

    private static final Logger LOGGER = LogManager.getLogger( CloudContext.class.getName() );

    protected CloudType type;
    protected String site;
    protected String workflowId;
    protected Set<String> hostNameSet;
    protected boolean triggerNotification;
    protected HashMap<String, String> hostNameToSliceNameHashMap;

    /*
     * @brief constructor
     *
     * @param t - cloud type
     * @param s - site
     * @param workflowId - workflow id
     *
     *
     */
    public CloudContext(CloudType t, String s, String workflowId) {
        type = t;
        site = s;
        this.workflowId = workflowId;
        hostNameSet = new HashSet<String>();
        triggerNotification = false;
        hostNameToSliceNameHashMap = new HashMap<>();
    }

    /*
     * @brief return site
     *
     * @return site
     */
    public String getSite() { return site; }

    /*
     * @brief return cloud type
     *
     * @return cloud type
     *
     */
    public CloudType getCloudType() { return type; }

    /*
     * @brief return triggerNotification flag
     *
     * @return triggerNotification flag
     */
    public boolean isTriggerNotification() {
        return triggerNotification;
    }

    /*
     * @brief set triggerNotification flag
     *
     * @param value - value for triggerNotification
     */
    public void setTriggerNotification(boolean value) {
        triggerNotification = value;
    }

    /*
     * @brief function to check if an instance with this hostname exists in this context
     *
     * @return true if hostname exists; false otherwise
     */
    public boolean containsHost(String hostname) {
        return hostNameSet.contains(hostname);
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
    abstract public Pair<Integer, Integer> processCompute(ComputeRequest request, int nameIndex, int spNameIndex, boolean isFutureRequest) throws Exception;

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
    abstract public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception;

    /*
     * @brief function to stitch to sdx and advertise a prefix for add operation and unstitch in case of delete
     *
     * @param hostname - hostname
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     *
     * @throws Exception in case of error
     *
     */
    abstract public int processStitchRequest(StitchRequest request, int nameIndex, boolean isFutureRequest) throws Exception;

    /*
     * @brief function to connect the link between source and destination subnet
     *
     * @param hostname - hostname
     * @param subnet1 - subnet1
     * @param subnet2 - subnet2
     * @param action - action
     * @param destHostName - destHostName
     *
     * @throws Exception in case of error
     *
     */
    abstract public void processNetworkRequestSetupStitchingAndRoute(String hostname, String ip,
                                                                     String subnet, NetworkRequest.ActionEnum action,
                                                                     String destHostName) throws Exception;

    /*
     * @brief function to process a network request;
     *
     * @param hostname - hostname
     *
     * @throws Exception in case of error
     *
     */
    abstract public void processNetworkRequestLink(String hostname, String subnet1, String subnet2, String bandwidth) throws Exception;

    /*
     * @brief function to check get status for the context
     *
     * @return JSONObject representing status
     */
    abstract public JSONObject getStatus() throws Exception;

    /*
     * @brief function to release all resources associated with this context
     */
    abstract public void stop() throws Exception;

    /*
     * @brief performs following periodic actions
     *        - Reload hostnames of all instances
     *        - Reload hostNameToSliceNameHashMap
     *        - Determine if notification to pegasus should be triggered
     *        - Build notification JSON object
     *
     * @return JSONObject representing notification for context to be sent to pegasus
     */
    abstract public JSONObject doPeriodic();

    /*
     * @brief function to check if an instance with this hostname exists in this context
     *
     * @return true if hostname exists; false otherwise
     */
    abstract public boolean containsSlice(String sliceName);

    /*
     * @brief function to generate JSONArray representing all the slice contexts held in this context
     *
     * @return JSONArray
     */
    abstract public JSONArray toJson();

    /*
     * @brief build the context from JSONArray read from database; invoked when contexts are loaded
     *        on mobius restart
     *
     * @param array - json array representing all the slice contexts
     */
    abstract public void fromJson(JSONArray array);

    /*
     * @brief add cloud specific info to JSON Object representing Context;
     *        JSON Object is saved to database
     *
     * @param object - json object representing Context
     */
    abstract public JSONObject addCloudSpecificDataToJson(JSONObject object);

    /*
     * @brief function to load cloud specific data from JSON Object representing Context
     *
     * @param object - json object representing Context
     */
    abstract public void loadCloudSpecificDataFromJson(JSONObject object);


    /*
     * @brief validate lease time; ignore validation if future request
     *
     * @param startTime - start time
     * @param endTime - end time
     * @param isFutureRequest - true if future request; false otherwise
     * @param maxDiffInSeconds - max allowed diff between start and end
     *
     * @throws exception in case of failure
     *
     */
    protected void validateLeasTime(String startTime, String endTime,
                                    boolean isFutureRequest, Long maxDiffInSeconds) throws Exception {
        LOGGER.debug("IN startTime=" + startTime + " endTime=" + endTime + " isFutureRequest=" + isFutureRequest + " maxDiffInSeconds=" + maxDiffInSeconds);
        if(startTime != null && endTime != null) {
            long currTime = System.currentTimeMillis();
            long beginTimestamp = Long.parseLong(startTime) * 1000;
            long endTimestamp = Long.parseLong(endTime) * 1000;

            if (beginTimestamp > currTime) {
                LOGGER.info("Future request to be started at " + beginTimestamp);
                throw new FutureRequestException("future request " + beginTimestamp);
            }

            // Ignore Start time check for requests triggered via periodic processing
            if (!isFutureRequest) {
                long diff = java.lang.Math.abs(currTime - beginTimestamp);
                if (diff > AllowedDeltaTimeInMsFromCurrentTime) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "startTime is before currentTime");
                }
            }

            if (endTimestamp < currTime) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "endTime is before currTime");
            }
            if (maxDiffInSeconds != null) {
                if ((endTimestamp - beginTimestamp) > (maxDiffInSeconds * 1000)) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Diff between endTime and startTime is more than " +
                            maxDiffInSeconds + " seconds");
                }
            } else {
                if (endTimestamp - beginTimestamp <= minimumTimeDifInMs) {
                    throw new MobiusException(HttpStatus.BAD_REQUEST,
                            "Diff between endTime and startTime is less than 24 hours");
                }
            }
        }
        LOGGER.debug("OUT");
    }
}
package org.renci.mobius.controllers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.*;
import org.apache.log4j.Logger;

abstract public class CloudContext {
    public enum CloudType {
        Chameleon,
        Exogeni,
        OSG,
        Unknown
    }
    public static final String NetworkName = "cmnw";
    public static final String StorageNetworkName = "stnw";
    public static final String StorageNameSuffix = "storage";
    public static final String NodeName = "Node";
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

    // Random string generator for read and write token
    public static String generateRandomString() {
        return RandomStringUtils.random( 10, true, true);
    }

    public static String generateSliceName(CloudType type, String user) {
        return "Mobius-" + type.name() + "-" + user + "-" + generateRandomString();
    }

    private static final Logger LOGGER = Logger.getLogger( CloudContext.class.getName() );

    protected CloudType type;
    protected String site;
    protected Set<String> hostNameSet;
    protected boolean triggerNotification;
    protected Multimap<Date, String> leaseEndTimeToSliceNameHashMap;
    protected HashMap<String, String> hostNameToSliceNameHashMap;


    public CloudContext(CloudType t, String s) {
        type = t;
        site = s;
        hostNameSet = new HashSet<String>();
        triggerNotification = false;

        leaseEndTimeToSliceNameHashMap = ArrayListMultimap.create();
        hostNameToSliceNameHashMap = new HashMap<>();
    }

    public String getSite() { return site; }
    public CloudType getCloudType() { return type; }
    public boolean isTriggerNotification() {
        return triggerNotification;
    }
    public void setTriggerNotification(boolean value) {
        triggerNotification = value;
    }
    public boolean containsHost(String hostname) {
        return hostNameSet.contains(hostname);
    }

    abstract public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception;
    abstract public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception;
    abstract public JSONObject getStatus() throws Exception;
    abstract public void stop() throws Exception;
    abstract public JSONObject doPeriodic();
    abstract public boolean containsSlice(String sliceName);
    abstract public JSONArray toJson();
    abstract public void fromJson(JSONArray array);


    protected void validateLeasTime(String startTime, String endTime, boolean isFutureRequest, Long maxDiffInSeconds) throws Exception {
        LOGGER.debug("validateLeasTime: IN");
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
                    throw new MobiusException(HttpStatus.BAD_REQUEST, "Diff between endTime and startTime is less than 24 hours");
                }
            }
        }
        LOGGER.debug("validateLeasTime: OUT");
    }

    protected String findSlice(ComputeRequest request) {
        LOGGER.debug("findSlice: IN");
        if(leaseEndTimeToSliceNameHashMap.size() == 0) {
            LOGGER.debug("findSlice: OUT - leaseEndTimeToSliceNameHashMap empty");
            return null;
        }

        if(request.getLeaseEnd() == null) {
            LOGGER.debug("findSlice: OUT - getLeaseEnd null");
            return null;
        }

        long timestamp = Long.parseLong(request.getLeaseEnd());
        Date expiry = new Date(timestamp * 1000);

        String sliceName = null;
        if(leaseEndTimeToSliceNameHashMap.containsKey(expiry)) {
            sliceName = leaseEndTimeToSliceNameHashMap.get(expiry).iterator().next();
        }
        LOGGER.debug("findSlice: OUT");
        return sliceName;
    }

    protected void handSliceNotFoundException(String sliceName) {
        LOGGER.debug("handSliceNotFoundException: IN");
        if(hostNameToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<HashMap.Entry<String, String>> iterator = hostNameToSliceNameHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        if(leaseEndTimeToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<Map.Entry<Date, String>> iterator = leaseEndTimeToSliceNameHashMap.entries().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Date, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        LOGGER.debug("handSliceNotFoundException: OUT");
    }
}
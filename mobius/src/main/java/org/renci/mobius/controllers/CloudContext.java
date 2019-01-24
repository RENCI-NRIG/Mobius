package org.renci.mobius.controllers;

import org.json.simple.JSONObject;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;

import java.util.HashSet;
import java.util.Set;

abstract public class CloudContext {
    public enum CloudType {
        Chameleon,
        Exogeni,
        OSG,
        Unknown
    }
    public static final String NetworkName = "network";
    public static final String StorageNetworkName = "storagenetwork";
    public static final String StorageNameSuffix = "storage";
    public static final String NodeName = "dataNode";
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

    public static String generateSliceName(CloudType type) {
        return "Mobius-" + type.name() + "-" + MobiusConfig.getInstance().getDefaultExogeniUser() + "-" + java.util.UUID.randomUUID().toString();
    }
    protected CloudType type;
    protected String site;
    protected Set<String> hostNameSet;
    protected boolean triggerNotification;

    public CloudContext(CloudType t, String s) {
        type = t;
        site = s;
        hostNameSet = new HashSet<String>();
        triggerNotification = false;
    }

    public String getSite() { return site; }
    public CloudType getCloudType() { return type; }

    public boolean isTriggerNotification() {
        return triggerNotification;
    }
    public void setTriggerNotification(boolean value) {
        triggerNotification = value;
    }

    abstract public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception;
    abstract public JSONObject getStatus() throws Exception;
    abstract public void stop() throws Exception;
    abstract public JSONObject doPeriodic();
    public boolean containsHost(String hostname) {
        return hostNameSet.contains(hostname);
    }
    abstract public void processStorageRequest(StorageRequest request, boolean isFutureRequest) throws Exception;
}
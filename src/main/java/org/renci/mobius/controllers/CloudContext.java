package org.renci.mobius.controllers;

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
    public static final Integer AllowedDeltaTimeInMsFromCurrentTime = 300000;
    public static final Long minimumTimeDifInMs = 86400000L; // 24 hours

    public static String generateSliceName(CloudType type) {
        return "Mobius-" + type.name() + "-" + MobiusConfig.getInstance().getDefaultExogeniUser() + "-" + java.util.UUID.randomUUID().toString();
    }
    protected CloudType type;
    protected String site;
    protected Set<String> hostNameSet;

    public CloudContext(CloudType t, String s) {
        type = t;
        site = s;
        hostNameSet = new HashSet<String>();
    }

    public String getSite() { return site; }
    public CloudType getCloudType() { return type; }

    abstract public int processCompute(String workflowId, ComputeRequest request, int nameIndex) throws Exception;
    abstract public String getStatus() throws Exception;
    abstract public void stop() throws Exception;
    abstract public String doPeriodic();
    public boolean containsHost(String hostname) {
        return hostNameSet.contains(hostname);
    }
    abstract public void processStorageRequest(StorageRequest request) throws Exception;
}
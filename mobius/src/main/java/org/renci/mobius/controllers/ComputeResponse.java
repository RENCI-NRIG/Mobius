package org.renci.mobius.controllers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ComputeResponse {
    private Integer nodeCount;
    private Integer stitchCount;
    private Map<String, String> hostNames;

    public ComputeResponse(Integer nodeCount, Integer stitchCount) {
        this.nodeCount = nodeCount;
        this.stitchCount = stitchCount;
        hostNames = new HashMap<>();
    }

    public void addHost(String host, String slicename) {
        hostNames.put(host, slicename);
    }

    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }

    public void setStitchCount(Integer stitchCount) { this.stitchCount = stitchCount; }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Integer getStitchCount() {
        return stitchCount;
    }

    public Map<String, String> getHostNames() {
        return hostNames;
    }
}

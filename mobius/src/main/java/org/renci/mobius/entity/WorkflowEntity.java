package org.renci.mobius.entity;

import java.io.Serializable;
import javax.persistence.*;

@Entity
public class WorkflowEntity implements Serializable  {
    @Id
    @Column(nullable = false)
    private String workflowId;
    
    @Column(columnDefinition = "TEXT", nullable = true)
    private String siteContextJson;

    @Column(nullable = false)
    private int nodeCount;

    @Column(nullable = false)
    private int storageCount;

    @Column(nullable = false)
    private int stitchCount;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String hostNamesJson;

    protected WorkflowEntity() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public WorkflowEntity(String workflowId, int nodeCount, int storageCount, int stitchCount, String siteContextJson, String hostNamesJson) {
        this.workflowId = workflowId;
        this.nodeCount = nodeCount;
        this.storageCount = storageCount;
        this.stitchCount = stitchCount;
        this.siteContextJson = siteContextJson;
        this.hostNamesJson = hostNamesJson;
    }

    public String getWorkflowId() {
        return this.workflowId;
    }

    public String getSiteContextJson() {
        return this.siteContextJson;
    }

    public int getNodeCount() { return nodeCount; }

    public int getStorageCount() { return storageCount; }

    public int getStitchCount() { return stitchCount; }

    public String getHostNamesJson() { return hostNamesJson; }
}

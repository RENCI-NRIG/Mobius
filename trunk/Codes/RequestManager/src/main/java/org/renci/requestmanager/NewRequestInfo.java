/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager;

/**
 *
 * @author anirban
 */
public class NewRequestInfo {
    
    private String wfUuid; // mandatory: workflow unique id
    private String templateType; // mandatory: Can be anything specified in RMConstants - "condor_pool", "condor_pool_storage" ....
    private int newCompRes; // optional: initial number of workers (HTCondor, Hadoop, MPI), if requested
    private int newStorage; // optional: initial amount of storage in GB, if requested
    private int newBandwidth; // optional: bandwidth between compute resources in Mb/s, if requested
    private String newImageUrl; // optional: image url, if requested
    private String newImageHash; // optional: image hash, if requested
    private String newImageName; // optional: image name, if requested
    private String newPostbootMaster; // optional: postboot script master, if requested
    private String newPostbootWorker; // optional: postboot script worker, if requested
    private LinkRequestInfo newLinkInfo; // optional linkInfo for stitchports, if required
    
    // NOTE: Not allowing user to pick domain

    public NewRequestInfo(String wfUuid, String templateType, int newCompRes, int newStorage, int newBandwidth, LinkRequestInfo newLinkInfo) {
        this.wfUuid = wfUuid;
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
        this.newLinkInfo = newLinkInfo;
    }
    
    public NewRequestInfo(String templateType, int newCompRes, int newStorage, int newBandwidth, LinkRequestInfo newLinkInfo) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
        this.newLinkInfo = newLinkInfo;
    }

    public NewRequestInfo(String wfUuid, String templateType) {
        this.wfUuid = wfUuid;
        this.templateType = templateType;
    }
    
    public NewRequestInfo(String templateType, int newCompRes) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
    }

    public NewRequestInfo(String templateType, int newCompRes, int newStorage, int newBandwidth) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
    }

    public NewRequestInfo(String templateType, int newCompRes, int newBandwidth) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newBandwidth = newBandwidth;
    }
    
    public NewRequestInfo(){
        
    }
    
    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public int getNewCompRes() {
        return newCompRes;
    }

    public void setNewCompRes(int newCompRes) {
        this.newCompRes = newCompRes;
    }

    public int getNewStorage() {
        return newStorage;
    }

    public void setNewStorage(int newStorage) {
        this.newStorage = newStorage;
    }

    public int getNewBandwidth() {
        return newBandwidth;
    }

    public void setNewBandwidth(int newBandwidth) {
        this.newBandwidth = newBandwidth;
    }

    public LinkRequestInfo getNewLinkInfo() {
        return newLinkInfo;
    }

    public void setNewLinkInfo(LinkRequestInfo newLinkInfo) {
        this.newLinkInfo = newLinkInfo;
    }

    public String getWfUuid() {
        return wfUuid;
    }

    public void setWfUuid(String wfUuid) {
        this.wfUuid = wfUuid;
    }
        
    
}

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
    private long newBandwidth; // optional: bandwidth between compute resources in Mb/s, if requested
    private String newImageUrl; // optional: image url, if requested
    private String newImageHash; // optional: image hash, if requested
    private String newImageName; // optional: image name, if requested
    private String newPostbootMaster; // optional: postboot script master, if requested
    private String newPostbootWorker; // optional: postboot script worker, if requested
    private LinkRequestInfo newLinkInfo; // optional linkInfo for stitchports, if required
    
    // NOTE: Not allowing user to pick domain

    public NewRequestInfo(String wfUuid, String templateType, int newCompRes, int newStorage, long newBandwidth, LinkRequestInfo newLinkInfo) {
        this.wfUuid = wfUuid;
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
        this.newLinkInfo = newLinkInfo;
    }
    
    public NewRequestInfo(String templateType, int newCompRes, int newStorage, long newBandwidth, LinkRequestInfo newLinkInfo) {
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

    public NewRequestInfo(String templateType, int newCompRes, int newStorage, long newBandwidth) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
    }

    public NewRequestInfo(String templateType, int newCompRes, long newBandwidth) {
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

    public long getNewBandwidth() {
        return newBandwidth;
    }

    public void setNewBandwidth(long newBandwidth) {
        this.newBandwidth = newBandwidth;
    }

    public String getNewImageUrl() {
        return newImageUrl;
    }

    public void setNewImageUrl(String newImageUrl) {
        this.newImageUrl = newImageUrl;
    }

    public String getNewImageHash() {
        return newImageHash;
    }

    public void setNewImageHash(String newImageHash) {
        this.newImageHash = newImageHash;
    }

    public String getNewImageName() {
        return newImageName;
    }

    public void setNewImageName(String newImageName) {
        this.newImageName = newImageName;
    }

    public String getNewPostbootMaster() {
        return newPostbootMaster;
    }

    public void setNewPostbootMaster(String newPostbootMaster) {
        this.newPostbootMaster = newPostbootMaster;
    }

    public String getNewPostbootWorker() {
        return newPostbootWorker;
    }

    public void setNewPostbootWorker(String newPostbootWorker) {
        this.newPostbootWorker = newPostbootWorker;
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

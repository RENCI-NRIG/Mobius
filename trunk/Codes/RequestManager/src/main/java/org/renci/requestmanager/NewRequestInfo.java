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
    
    private String templateType; // Can be "HTCondor", "Hadoop", "MPI"
    private int newCompRes; // initial number of workers (HTCondor, Hadoop, MPI)
    private int newStorage; // optional: initial amount of storage in GB
    private int newBandwidth; // optional: bandwidth between compute resources in Mb/s
    private LinkRequestInfo newLinkInfo; // optional linkInfo for stitchports

    public NewRequestInfo(String templateType, int newCompRes, int newStorage, int newBandwidth, LinkRequestInfo newLinkInfo) {
        this.templateType = templateType;
        this.newCompRes = newCompRes;
        this.newStorage = newStorage;
        this.newBandwidth = newBandwidth;
        this.newLinkInfo = newLinkInfo;
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
    
    
    
    
    
}

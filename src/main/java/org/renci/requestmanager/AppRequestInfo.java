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
public class AppRequestInfo {
    
    private String orcaSliceID; //sliceID to contact ExoGENI/ORCA for manifest and sending modify requests
    private LinkRequestInfo linkReq; //link request  ; reserved for slice modify support when we add link after slice is up
    private ModifyRequestInfo modifyReq; //modify request
    private NewRequestInfo newReq; // initial request
    private boolean processed; // app request processed or not

    public AppRequestInfo(String orcaSliceID, LinkRequestInfo linkReq, ModifyRequestInfo modifyReq, NewRequestInfo newReq) {
        this.orcaSliceID = orcaSliceID;
        this.linkReq = linkReq;
        this.modifyReq = modifyReq;
        this.newReq = newReq;
        this.processed = false;
    }

    public AppRequestInfo(String orcaSliceID, LinkRequestInfo linkReq, ModifyRequestInfo modifyReq, NewRequestInfo newReq, boolean processed) {
        this.orcaSliceID = orcaSliceID;
        this.linkReq = linkReq;
        this.modifyReq = modifyReq;
        this.newReq = newReq;
        this.processed = processed;
    }
    
    public String getOrcaSliceID() {
        return orcaSliceID;
    }

    public void setOrcaSliceID(String orcaSliceID) {
        this.orcaSliceID = orcaSliceID;
    }

    public LinkRequestInfo getLinkReq() {
        return linkReq;
    }

    public void setLinkReq(LinkRequestInfo linkReq) {
        this.linkReq = linkReq;
    }

    public ModifyRequestInfo getModifyReq() {
        return modifyReq;
    }

    public void setModifyReq(ModifyRequestInfo modifyReq) {
        this.modifyReq = modifyReq;
    }

    public NewRequestInfo getNewReq() {
        return newReq;
    }

    public void setNewReq(NewRequestInfo newReq) {
        this.newReq = newReq;
    }
    
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    
}

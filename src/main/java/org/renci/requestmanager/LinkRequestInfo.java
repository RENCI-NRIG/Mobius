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
public class LinkRequestInfo {
    
    private String wfUuid; // workflow unique id
    private String linkId; // link id to be used by entity requesting the link
    private int linkBandwidth; // bandwidth in Mb/s; say to the stitchport
    
    // following is the identifier in the stitchport registry; 
    // querying with this identifier gives the url of the stitchport mapper service; 
    // querying that service gives IP level info, vlan tag, port etc.
    private String stitchPortID;

    public LinkRequestInfo(String wfUuid, String linkId, int linkBandwidth, String stitchPortID) {
        this.wfUuid = wfUuid;
        this.linkId = linkId;
        this.linkBandwidth = linkBandwidth;
        this.stitchPortID = stitchPortID;
    }

    public LinkRequestInfo(String wfUuid, String linkId, String stitchPortID) {
        this.wfUuid = wfUuid;
        this.linkId = linkId;
        this.linkBandwidth = 10; // 10Mb/s default
        this.stitchPortID = stitchPortID;
    }    

    public LinkRequestInfo(){
        
    }
    
    public String getWfUuid() {
        return wfUuid;
    }

    public void setWfUuid(String wfUuid) {
        this.wfUuid = wfUuid;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getLinkBandwidth() {
        return linkBandwidth;
    }

    public void setLinkBandwidth(int linkBandwidth) {
        this.linkBandwidth = linkBandwidth;
    }

    public String getStitchPortID() {
        return stitchPortID;
    }

    public void setStitchPortID(String stitchPortID) {
        this.stitchPortID = stitchPortID;
    }
    
    
    
}

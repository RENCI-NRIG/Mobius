/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager;

/**
 *
 * @author anirbanmandal
 */
public class StitchportInfo {
    
    private String orcaSliceID;
    private String stitchPortID;
    int label; // vlan tag
    String port; // port
    // for auto IP 
    String firstSubnetIP; // "10.3.4.56"
    int firstSubnetMask; // 24

    public StitchportInfo(String orcaSliceID, String stitchPortID) {
        this.orcaSliceID = orcaSliceID;
        this.stitchPortID = stitchPortID;
    }

    public StitchportInfo(String orcaSliceID, String stitchPortID, int label, String port, String firstSubnetIP, int firstSubnetMask) {
        this.orcaSliceID = orcaSliceID;
        this.stitchPortID = stitchPortID;
        this.label = label;
        this.port = port;
        this.firstSubnetIP = firstSubnetIP;
        this.firstSubnetMask = firstSubnetMask;
    }

    public StitchportInfo() {
        
    }
    
    public String getOrcaSliceID() {
        return orcaSliceID;
    }

    public void setOrcaSliceID(String orcaSliceID) {
        this.orcaSliceID = orcaSliceID;
    }

    public String getStitchPortID() {
        return stitchPortID;
    }

    public void setStitchPortID(String stitchPortID) {
        this.stitchPortID = stitchPortID;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getFirstSubnetIP() {
        return firstSubnetIP;
    }

    public void setFirstSubnetIP(String firstSubnetIP) {
        this.firstSubnetIP = firstSubnetIP;
    }

    public int getFirstSubnetMask() {
        return firstSubnetMask;
    }

    public void setFirstSubnetMask(int firstSubnetMask) {
        this.firstSubnetMask = firstSubnetMask;
    }
    
    
    
    
    
}

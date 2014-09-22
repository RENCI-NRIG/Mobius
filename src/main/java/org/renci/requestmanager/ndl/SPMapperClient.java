/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import java.util.ArrayList;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class SPMapperClient {
    
    public class SPInfo{
        ArrayList<Integer> vlanTagSet = null;
        ArrayList<String> portSet = null;
        ArrayList<String> allowedIPSet = null;
        ArrayList<String> disallowedIPSet = null;
        
        // TODO: Add subnet information

        public SPInfo(){
            
        }
        
        public ArrayList<Integer> getVlanTagSet() {
            return vlanTagSet;
        }

        public void setVlanTagSet(ArrayList<Integer> vlanTagSet) {
            this.vlanTagSet = vlanTagSet;
        }

        public ArrayList<String> getPortSet() {
            return portSet;
        }

        public void setPortSet(ArrayList<String> portSet) {
            this.portSet = portSet;
        }

        public ArrayList<String> getAllowedIPSet() {
            return allowedIPSet;
        }

        public void setAllowedIPSet(ArrayList<String> allowedIPSet) {
            this.allowedIPSet = allowedIPSet;
        }

        public ArrayList<String> getDisallowedIPSet() {
            return disallowedIPSet;
        }

        public void setDisallowedIPSet(ArrayList<String> disallowedIPSet) {
            this.disallowedIPSet = disallowedIPSet;
        }
        
        
        
    }
    
    public SPInfo getSPInfo(String stitchPortID){
        
        String stitchPortMapperUrl = querySPRegistry(stitchPortID);
        
        if(stitchPortMapperUrl == null){
            stitchPortMapperUrl = RMConstants.defaultSPMapperUrl;
        }
        // TODO: query Stitchport mapper service
        SPInfo spInfo = null;
        
        spInfo = new SPInfo();
        ArrayList<Integer> vlanTagSet = new ArrayList<Integer>();
        vlanTagSet.add(1499);
        ArrayList<String> portSet = new ArrayList<String>();
        portSet.add("http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/4/ethernet");
        ArrayList<String> allowedIPSet = new ArrayList<String>();
        allowedIPSet.add("172.16.1.101");
        
        spInfo.setVlanTagSet(vlanTagSet);
        spInfo.setPortSet(portSet);
        spInfo.setAllowedIPSet(allowedIPSet);
        // TODO set subnet
        
        return spInfo;
        
    }
    
    /*
    * Returns the url of the stitchport mapper from the stitchport registry
    */
    private String querySPRegistry(String stitchPortID){
        return null;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.ndl;

import java.util.ArrayList;
import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.BroadcastNetwork;
import org.renci.ahab.libndl.resources.request.ComputeNode;

/**
 *
 * @author anirbanmandal
 */
public class NodeGroup {
    
    private int nodeGroupSize = 0;
    private int maxNodeGroupSize = 0;
    private ArrayList<ComputeNode> nodeList = new ArrayList<ComputeNode>();
    private Slice slice;
    private String nodeNamePrefixDefault = "workers";
    
    public NodeGroup(Slice s){
        slice = s;        
    }
    
    public NodeGroup(int count, String nodeNamePrefix, Slice s){        
        slice = s;
        nodeGroupSize = count;
        for(int i = 0; i < nodeGroupSize; i++){
            ComputeNode n = slice.addComputeNode(nodeNamePrefix + "-" + i);
            nodeList.add(n);
        }        
    }
    
    public NodeGroup(int count, String nodeNamePrefix, Slice s, BroadcastNetwork net){        
        slice = s;
        nodeGroupSize = count;
        for(int i = 0; i < nodeGroupSize; i++){
            ComputeNode n = slice.addComputeNode(nodeNamePrefix + "-" + i);
            nodeList.add(n);
            net.stitch(n);
        }        
    }
    
    /*
    Set image for all nodes in a nodegroup
    */
    public void setImage(String url, String hash, String name){        
        for(ComputeNode n : nodeList){
            n.setImage(url, hash, name);
        }        
    }
    
    /*
    Set domain for all nodes in a nodegroup
    */
    public void setDomain(String domain){
        for(ComputeNode n : nodeList){
            n.setDomain(domain);
        }
    }
    
    /*
    Set postboot script for all nodes in a nodegroup
    */
    public void setPostBootScript(String postboot){
        for(ComputeNode n : nodeList){
            n.setPostBootScript(postboot);
        }
    }
    
    /*
    Set slice for a nodegroup
    */
    public void setSlice(Slice s){
        slice = s;
    }
    
    /*
    Set node count for a nodegroup. This will also create the nodes in the nodegroup and assumes that a slice is already set
    */
    public void setNodeCount(int count){
        nodeGroupSize = count;
        for(int i = 0; i < nodeGroupSize; i++){
            ComputeNode n = slice.addComputeNode(nodeNamePrefixDefault + "-" + i);
            nodeList.add(n);
        }    
    }
    
    /*
    Attaches all nodes of a nodegroup to a broadcast network
    */
    public void attachToBroadcastNetwork(BroadcastNetwork net){
        for(ComputeNode n : nodeList){
            net.stitch(n);
        }
    }
    
    /*
    Set max size of nodegroup
    */
    public void setMaxNodeCount(int max){
        maxNodeGroupSize = max;
    }
    
    /*
    Set type of node for all nodes in a nodegroup; for eg. "XO Large"
    */
    public void setNodeType(String type){
        for(ComputeNode n : nodeList){
            n.setNodeType(type);
        }
    }
    
}

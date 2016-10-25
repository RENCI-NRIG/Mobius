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
public class ModifyRequestInfo {
    
    private String wfUuid; // workflow unique id
    private String orcaSliceId; // orca sliceID
    
    //type of modify
    private String modifyType; // 'modifyCompute' or 'modifyNetwork'
    
    //These are for compute elasticity (ADAMANT)
    private int deadline; // time from unix epoch
    private int numCurrentRes; // number of compute resources that application thinks is available now
    private int deadlineDiff; // +/- seconds by which application thinks it will miss deadline
    private int numResReqToMeetDeadline; // // number of compute resources that application thinks it needs to meet deadline
    private int numResUtilMax; // number of compute resources that application thinks can utilize 100% for the next hour
    
    //These are for flow elasticity (sdx control)
    private String endpointSrc; // source site for data flow
    private String endpointDst; // destination site for data flow
    private int flowPriority; // priority of the flow
    private String exchangeName; // name of amqp exchange where notification needs to be sent
    private String bindingKey; // amqp binding key for the notification
    
    //Other QoS parameters later

    public String getWfUuid() {
        return wfUuid;
    }

    public void setWfUuid(String wfUuid) {
        this.wfUuid = wfUuid;
    }

    public String getOrcaSliceId() {
        return orcaSliceId;
    }

    public void setOrcaSliceId(String orcaSliceId) {
        this.orcaSliceId = orcaSliceId;
    }

    public String getModifyType() {
        return modifyType;
    }

    public void setModifyType(String modifyType) {
        this.modifyType = modifyType;
    }
    
    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getNumCurrentRes() {
        return numCurrentRes;
    }

    public void setNumCurrentRes(int numCurrentRes) {
        this.numCurrentRes = numCurrentRes;
    }

    public int getDeadlineDiff() {
        return deadlineDiff;
    }

    public void setDeadlineDiff(int deadlineDiff) {
        this.deadlineDiff = deadlineDiff;
    }

    public int getNumResReqToMeetDeadline() {
        return numResReqToMeetDeadline;
    }

    public void setNumResReqToMeetDeadline(int numResReqToMeetDeadline) {
        this.numResReqToMeetDeadline = numResReqToMeetDeadline;
    }

    public int getNumResUtilMax() {
        return numResUtilMax;
    }

    public void setNumResUtilMax(int numResUtilMax) {
        this.numResUtilMax = numResUtilMax;
    }

    public String getEndpointSrc() {
        return endpointSrc;
    }

    public void setEndpointSrc(String endpointSrc) {
        this.endpointSrc = endpointSrc;
    }

    public String getEndpointDst() {
        return endpointDst;
    }

    public void setEndpointDst(String endpointDst) {
        this.endpointDst = endpointDst;
    }

    public int getFlowPriority() {
        return flowPriority;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }
    
    
    
    
}

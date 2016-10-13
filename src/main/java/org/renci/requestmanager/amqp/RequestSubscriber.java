/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.renci.requestmanager.AppRequestInfo;
import org.renci.requestmanager.DeleteRequestInfo;
import org.renci.requestmanager.LinkRequestInfo;
import org.renci.requestmanager.ModifyRequestInfo;
import org.renci.requestmanager.NewRequestInfo;
import org.renci.requestmanager.RMState;

/**
 *
 * @author anirban
 */
public class RequestSubscriber implements Runnable {

    // There is one AMQP queue per slice for resouce requests
    // This queue/slice name is read from configuration file when request manager starts
    private static String QUEUE_NAME = null; 
    
    protected ConnectionFactory factory = null;
    
    protected Logger logger = null;
    
    private static Properties rmProperties = null;

    public RequestSubscriber(Properties rmProps) throws Exception{
        
        logger = Logger.getLogger(this.getClass());
        
        this.rmProperties = rmProps;
        
        setupAMQPFactory(rmProperties);
        
    }    

    public void run() {
        
        try {
            
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            
            // Purging anything in the queue from old runs
            channel.queuePurge(QUEUE_NAME);
            
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(QUEUE_NAME, true, consumer);

            while (true) {
                
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received '" + message + "'");
              
                //Publish to display exchange about the request
                DisplayPublisher dp;
                try{
                    dp = new DisplayPublisher(rmProperties);
                    dp.publishRMMessages(message);
                }
                catch (Exception ex){
                    logger.error("Exception while publishing to display exchange");
                }
                
                // Parse message
                try {
                    AppRequestInfo appReq = parseAndCreateAppRequest(message);
                    // Add it to app request queue
                    if(appReq != null){
                        RMState rmState = RMState.getInstance();
                        
                        // Check if there is an unprocessed new request for same (sliceID, wfuuid) combo
                        if(appReq.getNewReq() != null){ // this is a new request
                            AppRequestInfo existingAppReq = rmState.findAppReqForNewFromAppReqQ(appReq.getOrcaSliceID(), appReq.getNewReq().getWfUuid());
                            if(existingAppReq == null){ // first unprocessed new req
                                // Push it to queue
                                rmState.addReqToAppReqQ(appReq);
                                logger.info("Added new request to appRequestQ...");
                            }
                            else{ // there was already an unprocessed new request for same (sliceID, wfuuid) combo
                                logger.info("Deleting existing unprocessed new request from appRequestQ...");
                                rmState.deleteReqFromAppReqQ(existingAppReq); // delete the existing unprocessed new request
                                // Push it to queue
                                rmState.addReqToAppReqQ(appReq);
                                logger.info("Added new request to appRequestQ...");
                            }
                        }
                        
                        // Checking whether there is an existing unprocessed modify request with same (sliceID, wfuuid) combo
                        if(appReq.getModifyReq() != null){ // this is a modify request
                            AppRequestInfo existingAppReq = rmState.findAppReqForModifyFromAppReqQ(appReq.getModifyReq().getOrcaSliceId(), appReq.getModifyReq().getWfUuid());
                            if(existingAppReq == null){ // first unprocessed modify request
                                // Push it to queue
                                rmState.addReqToAppReqQ(appReq);
                                logger.info("Added modify request to appRequestQ...");
                            }
                            else{ // there was already an unprocessed modify request for same (sliceID, wfuuid) combo
                                rmState.deleteReqFromAppReqQ(existingAppReq); // delete the existing unprocessed modify request
                                logger.info("Deleting existing unprocessed modify request from appRequestQ...");
                                // Push it to queue
                                rmState.addReqToAppReqQ(appReq);
                                logger.info("Added modify request to appRequestQ...");
                            }
                        }
                        
                        if(appReq.getDeleteReq() != null){ // this is a delete request
                            // No need to check if there is an existing delete request; I could; but not required
                            rmState.addReqToAppReqQ(appReq);
                            logger.info("Added delete request to appRequestQ...");
                        }
                        
                        
                    }
                    System.out.println("DONE...");
                }
                catch (Exception ex){
                    logger.error("Exception while getting message from AMQP queue or while parsing request, continuing..." + ex);
                    ex.printStackTrace();
                }
             
            }
            
        } catch (Exception ex) {
            logger.error("Exception while getting message from AMQP queue: " + ex);
            ex.printStackTrace();
        }
        
    }
    
    /*
    * Sets up AMQP factory for making connections
    */
    private void setupAMQPFactory(Properties rmProps) throws Exception{
        
        // TODO: Read from rmProps the following properties
        
        factory = new ConnectionFactory();
        //factory.setHost("gaul.isi.edu");
        factory.setHost("stewie.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        //factory.setPassword("adamant123");
        factory.setPassword("panorama123");       
        QUEUE_NAME = "testRequestQ"; // populated from rmProps
        
    }
    
    private AppRequestInfo parseAndCreateAppRequest(String amqpMesg){
        
        logger.info("AMQP message received = \n " + amqpMesg);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(amqpMesg);
        } catch (ParseException ex) {
            logger.info("Exception while parsing JSON amqp message " + ex);
            return null;
        }

        logger.info("Done parsing JSON message");
        
        AppRequestInfo appReq = null;
        
        String reqType = (String) jsonObject.get("requestType");
        logger.info("Resource request type: " + reqType);
        
        if(reqType.equalsIgnoreCase("new")){
            
            // Check mandatory fields
            String requestTemplateType = (String) jsonObject.get("req_templateType");
            if(requestTemplateType == null){
                logger.error("new resource request has to have a req_templateType");
                return null;
            }
            
            String requestSliceID = (String) jsonObject.get("req_sliceID");
            if(requestSliceID == null){
                logger.error("new resource request has to have a req_sliceID");
                return null;
            }
            
            String requestWfuuid = (String) jsonObject.get("req_wfuuid");
            if(requestWfuuid == null){
                logger.error("new resource request has to have a req_wfuuid");
                return null;
            }
            
            NewRequestInfo newReq = new NewRequestInfo();
            newReq.setTemplateType(requestTemplateType);
            newReq.setWfUuid(requestWfuuid);
            // Will pass requestSliceID as parameter to AppRequestInfo constructor
            
            logger.info("requestTemplateType = " + requestTemplateType);
            logger.info("requestWfUuid = " + requestWfuuid);
            logger.info("requestSliceID = " + requestSliceID);
                    
            // compute, storage and bandwidth
            if(jsonObject.containsKey("req_numWorkers")){
                Long requestNumWorkers = (Long) jsonObject.get("req_numWorkers");
                logger.info("requestNumWorkers = " + requestNumWorkers.intValue());
                newReq.setNewCompRes(requestNumWorkers.intValue());
            }
            else{
                newReq.setNewCompRes(-1);
            }
            
            if(jsonObject.containsKey("req_storage")){
                Long requestStorage = (Long) jsonObject.get("req_storage");
                logger.info("requestStorage = " + requestStorage.intValue());
                newReq.setNewStorage(requestStorage.intValue());
            }
            else{
                newReq.setNewStorage(-1);
            }
            
            if(jsonObject.containsKey("req_BW")){
                Long requestBW = (Long) jsonObject.get("req_BW");
                logger.info("requestBW = " + requestBW.longValue());
                newReq.setNewBandwidth(requestBW.longValue());
            }
            else{
                newReq.setNewBandwidth(-1);
            }
          
            
            // image properties
            if(jsonObject.containsKey("req_imageUrl") && jsonObject.containsKey("req_imageHash") && jsonObject.containsKey("req_imageName")){
                String requestImageUrl = (String) jsonObject.get("req_imageUrl");
                String requestImageHash = (String) jsonObject.get("req_imageHash");
                String requestImageName = (String) jsonObject.get("req_imageName");
                newReq.setNewImageUrl(requestImageUrl);
                newReq.setNewImageHash(requestImageHash);
                newReq.setNewImageName(requestImageName);
                logger.info("requestImageUrl = " + requestImageUrl + " | " + "requestImageHash = " + requestImageHash + " | " + "requestImageName = " + requestImageName);
            }
            else{
                newReq.setNewImageUrl(null);
                newReq.setNewImageHash(null);
                newReq.setNewImageName(null);
            }
            
            // postboot scripts
            if(jsonObject.containsKey("req_postbootMaster")){
                String requestPostbootMaster = (String) jsonObject.get("req_postbootMaster");
                newReq.setNewPostbootMaster(requestPostbootMaster);
            }
            else{
                newReq.setNewPostbootMaster(null);
            }
            
            if(jsonObject.containsKey("req_postbootWorker")){
                String requestPostbootWorker = (String) jsonObject.get("req_postbootWorker");
                newReq.setNewPostbootWorker(requestPostbootWorker);
            }
            else{
                newReq.setNewPostbootWorker(null);
            }
            
            
            // Link request
            if(jsonObject.containsKey("req_linkID") && jsonObject.containsKey("req_linkBW") && jsonObject.containsKey("req_stitchportID")){
                String requestLinkID = (String) jsonObject.get("req_linkID");
                Long requestLinkBW = (Long) jsonObject.get("req_linkBW");
                String requestStitchportID = (String) jsonObject.get("req_stitchportID");
                
                LinkRequestInfo linkInfo = new LinkRequestInfo();
                linkInfo.setLinkId(requestLinkID);
                linkInfo.setLinkBandwidth(requestLinkBW.longValue());
                linkInfo.setStitchPortID(requestStitchportID);
                linkInfo.setWfUuid(requestWfuuid);
                
                logger.info("*******: stitchportID received = " + requestStitchportID);
                
                newReq.setNewLinkInfo(linkInfo);
                
            }
            else if(jsonObject.containsKey("req_linkID") && jsonObject.containsKey("req_stitchportID")){
                String requestLinkID = (String) jsonObject.get("req_linkID");
                String requestStitchportID = (String) jsonObject.get("req_stitchportID");
                
                LinkRequestInfo linkInfo = new LinkRequestInfo();
                linkInfo.setLinkId(requestLinkID);
                linkInfo.setLinkBandwidth(-1);
                linkInfo.setStitchPortID(requestStitchportID);
                linkInfo.setWfUuid(requestWfuuid);
                
                logger.info("*******: stitchportID received = " + requestStitchportID);
                
                newReq.setNewLinkInfo(linkInfo);
            }
            else if(jsonObject.containsKey("req_stitchportID")){
                String requestLinkID = UUID.randomUUID().toString();
                String requestStitchportID = (String) jsonObject.get("req_stitchportID");
                
                LinkRequestInfo linkInfo = new LinkRequestInfo();
                linkInfo.setLinkId(requestLinkID);
                linkInfo.setLinkBandwidth(-1);
                linkInfo.setStitchPortID(requestStitchportID);
                linkInfo.setWfUuid(requestWfuuid);
                
                logger.info("*******: stitchportID received = " + requestStitchportID);
                
                newReq.setNewLinkInfo(linkInfo);
            }
            else {
                
                logger.info("*******: stitchportID received = null");
                newReq.setNewLinkInfo(null);
            }
            
            appReq = new AppRequestInfo(requestSliceID, null, null, newReq, null);
            
            
        }
        else if(reqType.equalsIgnoreCase("modifyCompute")){
            
            // TODO: parse and create modifyrequest
            // Check mandatory fields
            String requestSliceID = (String) jsonObject.get("req_sliceID");
            if(requestSliceID == null){
                logger.error("modifyCompute request has to have a req_sliceID");
                return null;
            }
            
            String requestWfuuid = (String) jsonObject.get("req_wfuuid");
            if(requestWfuuid == null){
                logger.error("modifyCompute request has to have a req_wfuuid");
                return null;
            }
            
            int reqNumCurrentRes = -1;
            if(!jsonObject.containsKey("req_numCurrentRes")){
                logger.error("modifyCompute request has to have a req_numCurrentRes");
                return null;
            }
            else {
                Long requestNumCurrentRes = (Long) jsonObject.get("req_numCurrentRes");
                logger.info("requestNumCurrentRes = " + requestNumCurrentRes.intValue());
                reqNumCurrentRes = requestNumCurrentRes.intValue();
            }
            
            int reqNumResToMeetDeadline = -1;
            if(!jsonObject.containsKey("req_numResReqToMeetDeadline")){
                logger.error("modifyCompute request has to have a req_numResReqToMeetDeadline");
                return null;
            }
            else {
                Long requestNumtResToMeetDeadline = (Long) jsonObject.get("req_numResReqToMeetDeadline");
                logger.info("requestNumResToMeetDeadline = " + requestNumtResToMeetDeadline.intValue());
                reqNumResToMeetDeadline = requestNumtResToMeetDeadline.intValue();
            }
            
            ModifyRequestInfo modifyComputeReq = new ModifyRequestInfo();
            modifyComputeReq.setOrcaSliceId(requestSliceID);
            modifyComputeReq.setWfUuid(requestWfuuid);
            modifyComputeReq.setNumCurrentRes(reqNumCurrentRes);
            modifyComputeReq.setNumResReqToMeetDeadline(reqNumResToMeetDeadline);
            
            if(jsonObject.containsKey("req_deadline")){
                Long requestDeadline = (Long) jsonObject.get("req_deadline");
                logger.info("requestDeadline = " + requestDeadline.intValue());
                modifyComputeReq.setDeadline(requestDeadline.intValue());
            }
            else {
                modifyComputeReq.setDeadline(-1);
            }
            
            if(jsonObject.containsKey("req_deadlineDiff")){
                Long requestDeadlineDiff = (Long) jsonObject.get("req_deadlineDiff");
                logger.info("requestDeadlineDiff = " + requestDeadlineDiff.intValue());
                modifyComputeReq.setDeadlineDiff(requestDeadlineDiff.intValue());
            }
            else {
                modifyComputeReq.setDeadlineDiff(-1);
            }
            
            if(jsonObject.containsKey("req_numResUtilMax")){
                Long requestNumResUtilMax = (Long) jsonObject.get("req_numResUtilMax");
                logger.info("requestNumResUtilMax = " + requestNumResUtilMax.intValue());
                modifyComputeReq.setNumResUtilMax(requestNumResUtilMax.intValue());
            }
            else {
                modifyComputeReq.setNumResUtilMax(-1);
            }
            
            modifyComputeReq.setModifyType(reqType);
            
            appReq = new AppRequestInfo(requestSliceID, null, modifyComputeReq, null, null);
            
        }
        else if(reqType.equalsIgnoreCase("modifyNetwork")){
            
            // TODO: when we have slice modify
            // check mandatory fields for modifyNetwork
            String requestSliceID = (String) jsonObject.get("req_sliceID");
            if(requestSliceID == null){
                logger.error("modifyNetwork request has to have a req_sliceID");
                return null;
            }
            
            String requestWfuuid = (String) jsonObject.get("req_wfuuid");
            if(requestWfuuid == null){
                logger.error("modifyNetwork request has to have a req_wfuuid");
                return null;
            }
            
            String requestEndpointSrc = null;
            if(!jsonObject.containsKey("req_endpointSrc")){
                logger.error("modifyNetwork request has to have a req_endpointSrc");
                return null;
            }
            else {
                requestEndpointSrc = (String) jsonObject.get("req_endpointSrc");
                logger.info("requestEndpointSrc = " + requestEndpointSrc);
            }
            
            String requestEndpointDst = null;
            if(!jsonObject.containsKey("req_endpointDst")){
                logger.error("modifyNetwork request has to have a req_endpointDst");
                return null;
            }
            else {
                requestEndpointDst = (String) jsonObject.get("req_endpointDst");
                logger.info("requestEndpointDst = " + requestEndpointDst);
            }
            
           
            int reqFlowPriority = -1;
            if(!jsonObject.containsKey("req_flowPriority")){
                logger.error("modifyNetwork request has to have a req_flowPriority");
                return null;
            }
            else {
                Long requestFlowPriority = (Long) jsonObject.get("req_flowPriority");
                logger.info("requestFlowPriority = " + requestFlowPriority.intValue());
                reqFlowPriority = requestFlowPriority.intValue();
            }
            
            ModifyRequestInfo modifyNetworkReq = new ModifyRequestInfo();
            modifyNetworkReq.setOrcaSliceId(requestSliceID);
            modifyNetworkReq.setWfUuid(requestWfuuid);
            modifyNetworkReq.setEndpointSrc(requestEndpointSrc);
            modifyNetworkReq.setEndpointDst(requestEndpointDst);
            modifyNetworkReq.setFlowPriority(reqFlowPriority);
            
            modifyNetworkReq.setModifyType(reqType);
            
            appReq = new AppRequestInfo(requestSliceID, null, modifyNetworkReq, null, null);
            
        }
        else if(reqType.equalsIgnoreCase("delete")){
            String requestSliceID = (String) jsonObject.get("req_sliceID");
            if(requestSliceID == null){
                logger.error("delete request has to have a req_sliceID");
                return null;
            }
            
            DeleteRequestInfo deleteReq = new DeleteRequestInfo();
            deleteReq.setOrcaSliceId(requestSliceID);
            
            // Note passing orcaSliceID in both appreqinfo and deletereqinfo; not required
            appReq = new AppRequestInfo(requestSliceID, null, null, null, deleteReq);
            
        }
        else {
            logger.error("Unknown request type...");
            return null;
        }
                
        return appReq;
        
    }
    
    public static void main(String[] argv) throws Exception {
        
        String testString =
                "{\n" +
                "	\"id\": 1,\n" +
                "        \"firstname\": \"Katerina\",\n" +
                "	\"languages\": [\n" +
                "		{ \"lang\":\"en\" , \"knowledge\":\"proficient\" }, \n" +
                "		{ \"lang\":\"fr\" , \"knowledge\":\"advanced\" }, \n" +
                "	]\n" +
                "	\"job\":{\n" +
                "                \"site\":\"www.javacodegeeks.com\",\n" +
                "                \"name\":\"Java Code Geeks\",\n" +
                "        }  \n" +
                "}";
        
        System.out.println(testString);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(testString);

        String firstName = (String) jsonObject.get("firstname");
        System.out.println("The first name is: " + firstName);
        
        Long id =  (Long) jsonObject.get("id");
        System.out.println("The id is: " + id.toString());

        JSONArray lang= (JSONArray) jsonObject.get("languages");

        for(int i=0; i<lang.size(); i++){
                System.out.println("The " + i + " element of the array: "+lang.get(i));
        }
        
        

    }    
    
    
    
    
}

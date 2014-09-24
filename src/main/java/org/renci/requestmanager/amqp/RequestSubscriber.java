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
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.renci.requestmanager.AppRequestInfo;
import org.renci.requestmanager.LinkRequestInfo;
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
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(QUEUE_NAME, true, consumer);

            while (true) {
                
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received '" + message + "'");
              
                // Parse message
                AppRequestInfo appReq = parseAndCreateAppRequest(message);
                
                // Add it to app request queue
                if(appReq != null){
                    RMState rmState = RMState.getInstance();
                    rmState.addReqToAppReqQ(appReq);
                    logger.info("Added new request to appRequestQ...");
                }
                
                System.out.println("DONE...");
             
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
        factory.setHost("gaul.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        factory.setPassword("adamant123");  
               
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
                
                newReq.setNewLinkInfo(linkInfo);
            }
            else {
                newReq.setNewLinkInfo(null);
            }
            
            appReq = new AppRequestInfo(requestSliceID, null, null, newReq);
            
            
        }
        else if(reqType.equalsIgnoreCase("modifyCompute")){
            
            // TODO: parse and create modifyrequest
           
        }
        else if(reqType.equalsIgnoreCase("modifyNetwork")){
            
            // TODO: when we have slice modify
        }
        
        /*
        Long id =  (Long) jsonObject.get("id");
        System.out.println("The id is: " + id.toString());

        JSONArray lang= (JSONArray) jsonObject.get("languages");

        for(int i=0; i<lang.size(); i++){
                System.out.println("The " + i + " element of the array: "+lang.get(i));
        }
        */
        
        
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

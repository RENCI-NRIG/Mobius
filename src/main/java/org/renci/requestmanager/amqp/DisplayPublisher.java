/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class DisplayPublisher {
    
    protected Logger logger = null;

    private static Properties rmProperties = null;
    
    protected ConnectionFactory factory = null;
    private static String EXCHANGE_NAME = null;    

    
    public class DisplayMessage {
        private String routingKey;
        private String message;

        public DisplayMessage(String routingKey, String message) {
            this.routingKey = routingKey;
            this.message = message;
        }

        private DisplayMessage() {
            
        }
        
        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
        
    }
    
    public DisplayPublisher(Properties rmProps) throws Exception{
        
        logger = Logger.getLogger(this.getClass());
        logger.info("DisplayPublisher created..");

        this.rmProperties = rmProps;

        setupAMQPFactory(rmProperties);
    }
    
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
               
        EXCHANGE_NAME = "testDisplayExchange"; // populated from rmProps
        
    }    
    
    public void publishInfraMessages(String orcaSliceID, String infraMessage){

        Connection connection = null;
        Channel channel = null;

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            
            DisplayMessage displayMessage = builMessageForInfraMessages(orcaSliceID, infraMessage);
            if(displayMessage == null){
                logger.info("Can't build amqp message for displaying Infrastructure action messages..");
                return;
            }
            String amqpMessage = displayMessage.getMessage();
            String routingKey = displayMessage.getRoutingKey();
            
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, amqpMessage.getBytes());
            logger.info(" [x] Sent to " + EXCHANGE_NAME + " '" + routingKey + "':'" + amqpMessage + "'");
        } 
        catch  (Exception e) {
            logger.error("Exception sending display data to exchange for infrastructure actions: " + e);
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
              try {
                connection.close();
              }
              catch (Exception ignore) {}
            }
        }        
        
        
    }
    
    private DisplayMessage builMessageForInfraMessages(String orcaSliceID, String infraMessage) {
        
        DisplayMessage dm = new DisplayMessage();
        
        JSONObject obj = new JSONObject(); 
        obj.put("display_inframessage", infraMessage);
        String message = obj.toJSONString();
        dm.setMessage(message);
        String routingKey = "adamant.infraaction." + orcaSliceID;
        dm.setRoutingKey(routingKey);
        
        return dm;
    }

    public void publishRMMessages(String inputMessage){

        Connection connection = null;
        Channel channel = null;

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            
            DisplayMessage displayMessage = parseAndBuildMessageForRMMessages(inputMessage);
            if(displayMessage == null){
                logger.info("Can't build amqp message for displaying RM messages..");
                return;
            }
            String amqpMessage = displayMessage.getMessage();
            String routingKey = displayMessage.getRoutingKey();
            
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, amqpMessage.getBytes());
            logger.info(" [x] Sent to " + EXCHANGE_NAME + " '" + routingKey + "':'" + amqpMessage + "'");
        } 
        catch  (Exception e) {
            logger.error("Exception sending display data to exchange for RM messages: " + e);
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
              try {
                connection.close();
              }
              catch (Exception ignore) {}
            }
        }

    }    

    private DisplayMessage parseAndBuildMessageForRMMessages(String inputMessage) {
        
        DisplayMessage dm = new DisplayMessage();
        
        logger.info("inputMessage  = \n " + inputMessage);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(inputMessage);
        } catch (ParseException ex) {
            logger.info("Exception while parsing JSON inputMessage " + ex);
            return null;
        }

        logger.info("Done parsing input JSON message");
        
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
            
            int numWorkers = 0;
            if(jsonObject.containsKey("req_numWorkers")){
                Long requestNumWorkers = (Long) jsonObject.get("req_numWorkers");
                logger.info("requestNumWorkers = " + requestNumWorkers.intValue());
                numWorkers = requestNumWorkers.intValue();
            }
            else{
                numWorkers = RMConstants.CondorDefaults.getDefaultNumWorkers();
            }
            
            String message = buildMessageNewReq(requestTemplateType, requestSliceID, numWorkers);
            dm.setMessage(message);
            String routingKey = "adamant.newrequest." + requestSliceID;
            dm.setRoutingKey(routingKey);
            
            return dm;
            
        }
        else if(reqType.equalsIgnoreCase("modifyCompute")){
            
            String requestSliceID = (String) jsonObject.get("req_sliceID");
            if(requestSliceID == null){
                logger.error("modifyCompute request has to have a req_sliceID");
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
            
            String message = buildMessageModifyComputeReq(requestSliceID, reqNumCurrentRes, reqNumResToMeetDeadline);
            dm.setMessage(message);
            String routingKey = "adamant.modifyrequest." + requestSliceID;
            dm.setRoutingKey(routingKey);
            
            return dm;
        }
        else{
            logger.error("Unknown request type..");
            return null;
        }
        
    }

    private String buildMessageNewReq(String templateType, String orcaSliceID, int numWorkers){

        // Build a test JSON message
        JSONObject obj = new JSONObject();
 
        obj.put("display_templateType", templateType);
        obj.put("display_orcaSliceID", orcaSliceID);        
        obj.put("display_numWorkers", numWorkers);

        System.out.println("JSON response = \n" + obj.toJSONString());

        return obj.toJSONString();

    }
            
    private String buildMessageModifyComputeReq(String orcaSliceID, int numCurrentRes, int numResToMeetDeadline){

        // Build a test JSON message
        JSONObject obj = new JSONObject();
 
        obj.put("display_orcaSliceID", orcaSliceID);        
        obj.put("display_numCurrentRes", numCurrentRes);
        obj.put("display_numResReqToMeetDeadline", numResToMeetDeadline);

        System.out.println("JSON response = \n" + obj.toJSONString());

        return obj.toJSONString();

    } 
    
    
    
}

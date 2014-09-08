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
import org.renci.requestmanager.RMState;

/**
 *
 * @author anirban
 */
public class ShadowQSubscriber implements Runnable {

    // There is one AMQP queue per slice for resouce requests
    // This slice name is read from configuration file when request manager starts
    private static String QUEUE_NAME = "testSlice"; 
    
    protected ConnectionFactory factory = null;
    
    protected Logger logger = null;
    
    private static Properties rmProperties = null;

    public ShadowQSubscriber(Properties rmProps) throws Exception{
        
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
                AppRequestInfo newReq = parseAndCreateAppRequest(message);
                
                // Add it to app request queue               
                RMState rmState = RMState.getInstance();
                rmState.addReqToAppReqQ(newReq);
        
                System.out.println("DONE...");
             
            }
            
        } catch (Exception ex) {
            logger.error("Exception while getting message from AMQP queue: " + ex);
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
               
        QUEUE_NAME = "testSlice"; // populated from rmProps
        
    }
    
    private AppRequestInfo parseAndCreateAppRequest(String amqpMesg){
        
        logger.info("AMQP message received = \n " + amqpMesg);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(amqpMesg);
        } catch (ParseException ex) {
            logger.info("Exception while parsing JSON message " + ex);
        }

        logger.info("Done parsing JSON");
        
        String reqType = (String) jsonObject.get("type");
        logger.info("Resource request type: " + reqType);
        
        if(reqType.equalsIgnoreCase("new")){
            
        }
        else if(reqType.equalsIgnoreCase("modifyCompute")){
           
        }
        else if(reqType.equalsIgnoreCase("modifyNetwork")){
            
        }
        
        Long id =  (Long) jsonObject.get("id");
        System.out.println("The id is: " + id.toString());

        JSONArray lang= (JSONArray) jsonObject.get("languages");

        for(int i=0; i<lang.size(); i++){
                System.out.println("The " + i + " element of the array: "+lang.get(i));
        }
        
        
        AppRequestInfo newReq = new AppRequestInfo("testSlice", null, null, null);
        
        return newReq;
        
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.simple.JSONObject;

/**
 *
 * @author anirban
 */
public class ModifyRequestTestPublisher {

    private static String QUEUE_NAME = "testRequestQ";

    public static void main(String[] argv) throws Exception {

        //buildMessageModifyRequest();
        //System.exit(0);
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("gaul.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        factory.setPassword("adamant123");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //String message = "Hello World!";               
        String message = buildMessageModifyRequest();
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        
        channel.close();
        connection.close();
        
    }

    private static String buildMessageModifyRequest(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("requestType", "modifyCompute");
        obj.put("req_sliceID", "testSlice-techX-1");
        obj.put("req_wfuuid", "0xcdfvgh");
        obj.put("req_numCurrentRes", 2);
        obj.put("req_numResReqToMeetDeadline", 4);
        
        
        // optional parameters
        obj.put("req_deadline", 1412217288);
        obj.put("req_deadlineDiff", 600);
        obj.put("req_numResUtilMax", 6);
        
        System.out.println("JSON request = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }    
    
    
}

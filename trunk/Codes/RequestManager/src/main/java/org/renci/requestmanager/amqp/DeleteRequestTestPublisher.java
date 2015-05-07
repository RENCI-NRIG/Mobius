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
public class DeleteRequestTestPublisher {

    private static String QUEUE_NAME = "testRequestQ";

    public static void main(String[] argv) throws Exception {

        //buildMessageModifyRequest();
        //System.exit(0);
        
        ConnectionFactory factory = new ConnectionFactory();
	//factory.setHost("gaul.isi.edu");
        factory.setHost("stewie.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        //factory.setPassword("adamant123");
        factory.setPassword("panorama123");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //String message = "Hello World!";               
        String message = buildMessageDeleteRequest();
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        
        channel.close();
        connection.close();
        
    }

    private static String buildMessageDeleteRequest(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("requestType", "delete");
        obj.put("req_sliceID", "testSlice-gec21-1");
        
        System.out.println("JSON request = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }       
    
    
}

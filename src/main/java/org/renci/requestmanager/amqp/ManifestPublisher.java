/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.amqp;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.json.simple.JSONObject;

/**
 *
 * @author anirban
 */
public class ManifestPublisher {
    
    // This class is responsible for publishing manifests to an exchange
    private static final String EXCHANGE_NAME = "testManifestExchange";

    public static void main(String[] argv) {
        
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("gaul.isi.edu");
            factory.setPort(5671);
            factory.useSslProtocol();
            factory.setUsername("anirban");
            factory.setPassword("adamant123");
            

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            String routingKey = getRouting(argv);
            String message = buildMessageManifestResponse();

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");

        }
        catch  (Exception e) {
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

    private static String getRouting(String[] strings){
        if (strings.length < 1)
            return "testSlice.0xcdfvgh";
        return strings[0];
    }

    private static String buildMessageManifestResponse(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("response_orcaSliceID", "testSlice");
        obj.put("response_sliceStatus", "ready");
        obj.put("response_numWorkersReady", 4);
        
        System.out.println("JSON response = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }    
    
}

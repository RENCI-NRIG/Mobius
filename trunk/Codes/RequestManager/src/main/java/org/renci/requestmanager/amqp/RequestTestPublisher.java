/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author anirban
 */
public class RequestTestPublisher {
    
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
        String message = buildMessageNewRequest();
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
        System.exit(0);
        
        try{     
            Thread.sleep(40000); // wait for 40 seconds
        } catch (InterruptedException ex) {
            System.out.println("thread interrupted");
            channel.close();
            connection.close();
        }
        
        message = buildMessageModifyRequest();
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        
        channel.close();
        connection.close();
        
    }
    
    private static String buildMessageNewRequest(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("requestType", "new");
        //obj.put("req_templateType", "condor_storage_sp_multi");
        //obj.put("req_templateType", "condor_sp_multi");
        obj.put("req_templateType", "condor");
        obj.put("req_sliceID", "testSlice");
        obj.put("req_wfuuid", "0xcdfvgh");
        
        // optional parameters
        
        obj.put("req_numWorkers", 2);
        //obj.put("req_storage", 50);
        //obj.put("req_BW", 100000000);
        //obj.put("req_imageUrl", "image url");
        //obj.put("req_imageHash", "image hash");
        //obj.put("req_imageName", "image name");
        //obj.put("req_postbootMaster", "master postboot script");
        //obj.put("req_postbootWorker", "worker postboot script");
        
        // optional link and stitchport info
        
        obj.put("req_linkID", "0xghlkjh");
        obj.put("req_linkBW", new Long(100000000));
        obj.put("req_stitchportID", "Cenic@ION");
        
        System.out.println("JSON request = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }

    private static String buildMessageModifyRequest(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("requestType", "modifyCompute");
        obj.put("req_sliceID", "testSlice");
        obj.put("req_wfuuid", "0xcdfvgh");
        obj.put("req_numCurrentRes", 4);
        obj.put("req_numResReqToMeetDeadline", 8);
        
        
        // optional parameters
        obj.put("req_deadline", 1412217288);
        obj.put("req_deadlineDiff", 600);
        obj.put("req_numResUtilMax", 6);
        
        System.out.println("JSON request = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }    
    
}

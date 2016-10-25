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


/**
 *
 * @author anirban
 */
public class FlowPrioritySetupPublisher {

        
    protected Logger logger = null;

    private static Properties rmProperties = null;
    
    protected ConnectionFactory factory = null;
    private static String EXCHANGE_NAME = null;    

    
    public FlowPrioritySetupPublisher(Properties rmProps) throws Exception{
        
        logger = Logger.getLogger(this.getClass());
        logger.info("FlowPrioritySetupPublisher created..");

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
        factory.setVirtualHost("panorama");
               
        EXCHANGE_NAME = "priority"; // populated from rmProps
        
    }    
    
    public void publishFlowPrioritySetupComplete(String exchangeName, String routingKey){

        Connection connection = null;
        Channel channel = null;

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            EXCHANGE_NAME = exchangeName;
            
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            
            String amqpMessage = "OK";
            
            logger.info(" [x] Sending to " + EXCHANGE_NAME + " '" + routingKey + "':'" + amqpMessage + "'");
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, amqpMessage.getBytes());
            logger.info(" [x] Sent to " + EXCHANGE_NAME + " '" + routingKey + "':'" + amqpMessage + "'");
        } 
        catch  (Exception e) {
            logger.error("Exception sending OK to exchange after network modify actions: " + e);
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
    
    
}

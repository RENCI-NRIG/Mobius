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
import org.renci.requestmanager.RMState;


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

        //setupAMQPFactory(rmProperties);
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
            
            RMState rmState = RMState.getInstance();
            factory = rmState.getFactory();
            
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
    
    public static void main(String[] argv) throws Exception{
        System.out.println("Hello Anirban");
        
        ConnectionFactory factory = new ConnectionFactory();
                
//                factory.setHost("stewie.isi.edu");
//                factory.setPort(5671);
//                factory.useSslProtocol();
//                factory.setUsername("anirban");
//                factory.setPassword("panorama123");
//                factory.setVirtualHost("panorama");   
                
                factory.setHost("147.72.248.11");
                factory.setPort(5672);
                //factory.useSslProtocol();
                factory.setUsername("anirban");
                factory.setPassword("panorama123");
                factory.setVirtualHost("panorama");
                
        
        Connection connection = null;

        try {
            for(int i = 0; i < 20; i++){
                
                connection = factory.newConnection();
                System.out.println("Done creating connection " + i);
                try {
                    Thread.sleep(1*1000);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {  
                    Thread.currentThread().interrupt();
                }
                connection.close();
            }
        } 
        catch  (Exception e) {
            System.out.println("Exception sending OK to exchange after network modify actions: " + e);
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

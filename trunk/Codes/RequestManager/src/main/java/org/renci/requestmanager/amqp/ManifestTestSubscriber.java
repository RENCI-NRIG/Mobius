/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

/**
 *
 * @author anirban
 */
public class ManifestTestSubscriber {
    
  private static final String EXCHANGE_NAME = "testManifestExchange";

  public static void main(String[] argv) {
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
	    //factory.setHost("gaul.isi.edu");
            factory.setHost("stewie.isi.edu");
            factory.setPort(5671);
            factory.useSslProtocol();
            factory.setUsername("anirban");
            //factory.setPassword("adamant123");
            factory.setPassword("panorama123");

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            String queueName = channel.queueDeclare().getQueue();
 
            String bindingKey = "testSlice.*";             
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, true, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                String routingKey = delivery.getEnvelope().getRoutingKey();

                System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");   
            }
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
    
    
}

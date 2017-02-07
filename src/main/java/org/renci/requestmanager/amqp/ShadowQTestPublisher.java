/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 *
 * @author anirban
 */
public class ShadowQTestPublisher {
    
        private final static String QUEUE_NAME = "testSlice";

        public static void main(String[] argv) throws Exception {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("XXXX.isi.edu");
            factory.setPort(5671);
            factory.useSslProtocol();
            factory.setUsername("xxxxxx");
            factory.setPassword("yyyyyy");

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");

            channel.close();
            connection.close();
      }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.ConnectionFactory;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.requestmanager.RMState;

/**
 *
 * @author anirban
 */
public class SetupAMQPConnection {

    
    protected Logger logger = null;
    
    public SetupAMQPConnection(Properties rmProps) throws Exception {
        
        logger = Logger.getLogger(this.getClass());
        logger.info("Creating new AMQPConnection..");
        
        setupAMQPFactory(rmProps);
        
    }
    
    public void setupAMQPFactory(Properties rmProps) throws Exception{
        
        // TODO: Read from rmProps the following properties
        
        RMState rmState = RMState.getInstance();
        
        ConnectionFactory factory = rmState.getFactory();
	//factory.setHost("gaul.isi.edu");
        factory.setHost("stewie.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        //factory.setPassword("adamant123");
        factory.setPassword("panorama123");
        factory.setVirtualHost("panorama");
        
        rmState.setFactory(factory);
        
    }
    
}

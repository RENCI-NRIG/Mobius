/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.ConnectionFactory;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.requestmanager.RMConstants;
import org.renci.requestmanager.RMState;

/**
 *
 * @author anirban
 */
public class SetupAMQPConnection implements RMConstants{

    
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
        
        if(rmProps.getProperty(AMQP_SERVER_NAME_PROP) != null){
            factory.setHost(rmProps.getProperty(AMQP_SERVER_NAME_PROP));
            logger.info("AMQP host: " + rmProps.getProperty(AMQP_SERVER_NAME_PROP));
        }
        else{
            logger.error("AMQP hostname missing");
        }
        
        if(rmProps.getProperty(AMQP_SERVER_PORT_PROP) != null){
            factory.setPort(Integer.parseInt(rmProps.getProperty(AMQP_SERVER_PORT_PROP)));
            logger.info("AMQP port: " + Integer.parseInt(rmProps.getProperty(AMQP_SERVER_PORT_PROP)));
        }
        else{
            logger.error("AMQP port number missing");
        }
        
        if(rmProps.getProperty(AMQP_SERVER_SSL_PROP) != null){
            String useSSLString = rmProps.getProperty(AMQP_SERVER_SSL_PROP);
            if(useSSLString.equalsIgnoreCase("true")){
                factory.useSslProtocol();
                logger.info("AMQP useSSL: " + "true");
            }
            else{
                logger.info("AMQP useSSL: " + "false");
            }
        }
        else{
            logger.info("AMQP useSSL: " + "false");
        }
        
        if(rmProps.getProperty(AMQP_USER_NAME_PROP) != null){
            factory.setUsername(rmProps.getProperty(AMQP_USER_NAME_PROP));
            logger.info("AMQP user: " + rmProps.getProperty(AMQP_USER_NAME_PROP));
        }
        else{
            logger.error("AMQP username missing");
        }
        
        if(rmProps.getProperty(AMQP_USER_PASSWORD_PROP) != null){
            factory.setPassword(rmProps.getProperty(AMQP_USER_PASSWORD_PROP));
            logger.info("AMQP password: " + rmProps.getProperty(AMQP_USER_PASSWORD_PROP));
        }
        else{
            logger.error("AMQP password missing");
        }
        
        if(rmProps.getProperty(AMQP_VIRTUAL_HOST_PROP) != null){
            factory.setVirtualHost(rmProps.getProperty(AMQP_VIRTUAL_HOST_PROP));
            logger.info("AMQP virtualhost: " + rmProps.getProperty(AMQP_VIRTUAL_HOST_PROP));
        }
        else{
            logger.info("AMQP virtualhost missing");
        }
        
        rmState.setFactory(factory);
        
    }
    
}

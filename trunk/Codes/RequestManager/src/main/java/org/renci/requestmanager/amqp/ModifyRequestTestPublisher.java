/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class ModifyRequestTestPublisher implements RMConstants{

    private static String QUEUE_NAME = "testRequestQ";
    private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
    private static final String PREF_FILE = ".rm.properties";
    private static Properties rmProps = null;
    
    
    public static void main(String[] argv) throws Exception {

        // This populates rmProps
        processPreferences();
        
        ConnectionFactory factory = new ConnectionFactory();
        Logger logger = Logger.getLogger(ModifyRequestTestPublisher.class.getName());
        
        
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
    

    /**
     * Read and process preferences file
     */
    protected static void processPreferences() {

            Properties p = System.getProperties();

            // properties can be under /etc/mm/mm.properties or under $HOME/.mm.properties
            // in that order of preference
            String prefFilePath = GLOBAL_PREF_FILE;

            try {
                    rmProps = loadPropertiesFromAnyFile(prefFilePath);
                    return;
            } catch (IOException ioe) {
                    System.err.println("Unable to load global config file " + prefFilePath + ", trying local file");
            }

            prefFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + PREF_FILE;
            try {
                    rmProps = loadPropertiesFromAnyFile(prefFilePath);
            } catch (IOException e) {
                    System.err.println("Unable to load local config file " + prefFilePath + ", exiting.");
                    System.exit(1);
            }
    }

    /**
     * loads properties from any file , given it's absolute path
     * @param fileName
     * @return
     * @throws IOException
     */
    private static Properties loadPropertiesFromAnyFile(String fileName) throws IOException {

        File prefs = new File(fileName);
        FileInputStream is = new FileInputStream(prefs);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        Properties p = new Properties();
        p.load(bin);
        bin.close();

        return p;

    }
    

    
}

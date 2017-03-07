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
import org.json.simple.JSONObject;
import org.renci.requestmanager.RMConstants;
import org.renci.requestmanager.ndl.AhabManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;

/**
 *
 * @author anirban
 */
public class ManifestPublisherOnDemand implements RMConstants{
    
    protected Logger logger = null;

    private static Properties rmProperties = null;
    
    protected ConnectionFactory factory = null;
    private static String EXCHANGE_NAME = null;
    

    public ManifestPublisherOnDemand(Properties rmProps) throws Exception{

            logger = Logger.getLogger(this.getClass());
            logger.info("ManifestPublisherOnDemand starting ...");

            this.rmProperties = rmProps;

            setupAMQPFactory(rmProperties);
    }

    /*
    * Sets up AMQP factory for making connections
    */
    private void setupAMQPFactory(Properties rmProps) throws Exception{
        
        factory = new ConnectionFactory();
        
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
       
        EXCHANGE_NAME = "testManifestExchange"; // populated from rmProps
        
    }    

    public void getManifestAndPublishManifestData(String orcaSliceID){
        
        OrcaManager orcaManager = new OrcaManager(rmProperties);
        AhabManager ahabManager = new AhabManager(rmProperties);
        
        // Get manifest from ExoGENI
        logger.info("ManifestPublisherOnDemand: Getting manifest form ExoGENI");
        String currManifest = orcaManager.getManifestFromORCA(orcaSliceID);
        if(currManifest == null){
            logger.error("Manifest is null for slice: " + orcaSliceID);
        }

        
        // Parse manifest using AHAB
        logger.info("ManifestPublisherOnDemand: Parsing manifest data by calling AHAB");
        String sliceState = ahabManager.getSliceManifestStatus(currManifest);
        int numActiveWorkers = ahabManager.getNumActiveWorkersInManifest(currManifest);                        
        // Get number of workers coming up
        int numTicketedWorkers = ahabManager.getNumTicketedWorkersInManifest(currManifest);
        int numNascentWorkers = ahabManager.getNumNascentWorkersInManifest(currManifest);
        int numProvisioningWorkers = numTicketedWorkers + numNascentWorkers;
        String masterPublicIP = ahabManager.getPublicIPMasterInManifest(currManifest);
        if(masterPublicIP == null){ // master not yet up
            masterPublicIP = "unknown";
        }
        
        // Publish relevant data to manifest exchange
        logger.info("ManifestPublisherOnDemand: Publishing manifest data to exchange");
        publishManifestData(orcaSliceID, sliceState, numActiveWorkers, numProvisioningWorkers, masterPublicIP);
        
    }
    
    
    private void publishManifestData(String orcaSliceID, String sliceState, int numActiveWorkers, int numProvisioningWorkers, String masterPublicIP){

        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            String routingKey = "adamant.manifest." + orcaSliceID;
            String message = buildMessageManifestResponseData(orcaSliceID, sliceState, numActiveWorkers, numProvisioningWorkers, masterPublicIP);

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
            logger.info(" [x] Sent '" + routingKey + "':'" + message + "'");
        } 
        catch  (Exception e) {
            logger.error("Exception sending manifest data to exchange: " + e);
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

    private String buildMessageManifestResponseData(String orcaSliceID, String sliceState, int numActiveWorkers, int numTicketedWorkers, String masterPublicIP){

        // Build a test JSON message
        JSONObject obj = new JSONObject();
        // mandatory
        obj.put("response_orcaSliceID", orcaSliceID);
        obj.put("response_sliceStatus", sliceState);
        obj.put("response_numWorkersReady", numActiveWorkers);
        obj.put("response_numWorkersProvisioning", numTicketedWorkers);
        obj.put("response_masterPublicIP", masterPublicIP);
        
        System.out.println("JSON response = \n" + obj.toJSONString());

        return obj.toJSONString();

    }    
    
    
}

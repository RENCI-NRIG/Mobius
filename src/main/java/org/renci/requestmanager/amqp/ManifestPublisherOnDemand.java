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
import org.renci.requestmanager.ndl.NdlLibManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;

/**
 *
 * @author anirban
 */
public class ManifestPublisherOnDemand {
    
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
        
        // TODO: Read from rmProps the following properties
        
        factory = new ConnectionFactory();
        factory.setHost("gaul.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        factory.setPassword("adamant123");  
               
        EXCHANGE_NAME = "testManifestExchange"; // populated from rmProps
        
    }    

    public void getManifestAndPublishManifestData(String orcaSliceID){
        
        OrcaManager orcaManager = new OrcaManager(rmProperties);
        NdlLibManager ndlManager = new NdlLibManager(rmProperties);
        
        // Get manifest from ExoGENI
        logger.info("ManifestPublisherOnDemand: Getting manifest form ExoGENI");
        String currManifest = orcaManager.getManifestFromORCA(orcaSliceID);
        if(currManifest == null){
            logger.error("Manifest is null for slice: " + orcaSliceID);
        }

        // Parse manifest using ndllib
        logger.info("ManifestPublisherOnDemand: Parsing manifest data by calling ndlLib");
        String sliceState = ndlManager.getSliceManifestStatus(currManifest);
        int numActiveWorkers = ndlManager.getNumActiveWorkersInManifest(currManifest);                        
        // Get number of workers coming up
        int numTicketedWorkers = ndlManager.getNumTicketedWorkersInManifest(currManifest);
        int numNascentWorkers = ndlManager.getNumNascentWorkersInManifest(currManifest);
        int numProvisioningWorkers = numTicketedWorkers + numNascentWorkers;

        // Publish relevant data to manifest exchange
        logger.info("ManifestPublisherOnDemand: Publishing manifest data to exchange");
        publishManifestData(orcaSliceID, sliceState, numActiveWorkers, numProvisioningWorkers);
        
    }
    
    
    private void publishManifestData(String orcaSliceID, String sliceState, int numActiveWorkers, int numProvisioningWorkers){

        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            String routingKey = "adamant.manifest." + orcaSliceID;
            String message = buildMessageManifestResponseData(orcaSliceID, sliceState, numActiveWorkers, numProvisioningWorkers);

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

    private String buildMessageManifestResponseData(String orcaSliceID, String sliceState, int numActiveWorkers, int numTicketedWorkers){

        // Build a test JSON message
        JSONObject obj = new JSONObject();
        // mandatory
        obj.put("response_orcaSliceID", orcaSliceID);
        obj.put("response_sliceStatus", sliceState);
        obj.put("response_numWorkersReady", numActiveWorkers);
        obj.put("response_numWorkersProvisioning", numTicketedWorkers);

        System.out.println("JSON response = \n" + obj.toJSONString());

        return obj.toJSONString();

    }    
    
    
}

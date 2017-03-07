/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager.amqp;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.renci.requestmanager.AppRequestInfo;
import org.renci.requestmanager.RMConstants;
import org.renci.requestmanager.RMState;
import org.renci.requestmanager.ndl.AhabManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;

/**
 *
 * @author anirban
 * This class is responsible for publishing manifests to an exchange
 */
public class ManifestPublisher implements RMConstants{
    
    private static ArrayList<Timer> timers = new ArrayList<Timer>();
    
    private static boolean noStart = false;

    protected RMState rmState = null;

    protected Logger logger = null;

    private static Properties rmProperties = null;
    
    protected ConnectionFactory factory = null;
    private static String EXCHANGE_NAME = null;
    

    public ManifestPublisher(Properties rmProps) throws Exception{

            logger = Logger.getLogger(this.getClass());
            logger.info("ManifestPublisher created..");

            this.rmProperties = rmProps;

            setupAMQPFactory(rmProperties);
            
            Timer timer = null;
            synchronized(timers) {
                    if (noStart)
                            return;
                    timer = new Timer("ManifestPublisherTask", true);
                    timers.add(timer);
            }
            
            timer.schedule(new ManifestPublisherTask(), 0, 60*1000); // run every 60 seconds
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
    
    private void allStop() {
        logger.info("Shutting down ManifestPublisher timer threads");
        synchronized(timers) {
                noStart=true;
                for (Timer t: timers) {
                        t.cancel();
                }
                timers.clear();
        }
    }
    
    class ManifestPublisherTask extends TimerTask {

        public void run() {

            try{

                logger.info("ManifestPublisher timer thread ..... : START");

                rmState = RMState.getInstance();

                ArrayList<String> currSliceIDQ = rmState.getSliceIDQ();

                synchronized(currSliceIDQ){

                    if(currSliceIDQ == null){
                        return; // nothing to do if there are no slices to watch
                    }
                    if(currSliceIDQ.size() <= 0){ // nothing in the queue
                        return;
                    }

                    OrcaManager orcaManager = new OrcaManager(rmProperties);
                    AhabManager ahabManager = new AhabManager(rmProperties);
                    
                    for(String currSliceID: currSliceIDQ){ // Go through all the slice ids
                        
                        // Get manifest from ExoGENI
                        logger.info("ManifestPublisher: Getting manifest form ExoGENI");
                        String currManifest = orcaManager.getManifestFromORCA(currSliceID);
                        if(currManifest == null){
                            logger.error("Manifest is null for slice: " + currSliceID);
                            continue;
                        }
                        
                        // Parse manifest using AHAB
                        
                        logger.info("ManifestPublisher: Parsing manifest data by calling AHAB");
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
                        logger.info("ManifestPublisher: Publishing manifest data to exchange");
                        publishManifestData(currSliceID, sliceState, numActiveWorkers, numProvisioningWorkers, masterPublicIP);
                        

                    } // end for 

                } // end synchronized                     

            } catch (Exception e){
                logger.error("Exception occured during Timer thread execution of ManifestPublisher: " , e);
                logger.error("Continuing execution beyond Exception");
                System.out.println("There was an exception during Timer thread execution of ManifestPublisher");
            }

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

            String routingKey = getRouting(argv);
            String message = buildMessageManifestResponse();

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");

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

    private static String getRouting(String[] strings){
        if (strings.length < 1)
            return "testSlice.0xcdfvgh";
        return strings[0];
    }

    private static String buildMessageManifestResponse(){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("response_orcaSliceID", "testSlice");
        obj.put("response_sliceStatus", "ready");
        obj.put("response_numWorkersReady", 4);
        
        System.out.println("JSON response = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    }    
    
}

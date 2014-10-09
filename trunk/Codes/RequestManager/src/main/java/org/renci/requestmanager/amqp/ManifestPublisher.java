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
import org.renci.requestmanager.RMState;
import org.renci.requestmanager.ndl.NdlLibManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;

/**
 *
 * @author anirban
 * This class is responsible for publishing manifests to an exchange
 */
public class ManifestPublisher {
    
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
        
        // TODO: Read from rmProps the following properties
        
        factory = new ConnectionFactory();
        factory.setHost("gaul.isi.edu");
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("anirban");
        factory.setPassword("adamant123");  
               
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
                    NdlLibManager ndlManager = new NdlLibManager(rmProperties);
                    
                    for(String currSliceID: currSliceIDQ){ // Go through all the slice ids
                        
                        // Get manifest from ExoGENI
                        logger.info("ManifestPublisher: Getting manifest form ExoGENI");
                        String currManifest = orcaManager.getManifestFromORCA(currSliceID);
                        if(currManifest == null){
                            logger.error("Manifest is null for slice: " + currSliceID);
                            continue;
                        }
                        
                        // Parse manifest using ndllib
                        logger.info("ManifestPublisher: Parsing manifest data by calling ndlLib");
                        String sliceState = ndlManager.getSliceManifestStatus(currManifest);
                        int numActiveWorkers = ndlManager.getNumActiveWorkersInManifest(currManifest);
                        
                        // Publish relevant data to manifest exchange
                        logger.info("ManifestPublisher: Publishing manifest data to exchange");
                        publishManifestData(currSliceID, sliceState, numActiveWorkers);
                        

                    } // end for 

                } // end synchronized                     

            } catch (Exception e){
                logger.error("Exception occured during Timer thread execution of ManifestPublisher: " , e);
                logger.error("Continuing execution beyond Exception");
                System.out.println("There was an exception during Timer thread execution of ManifestPublisher");
            }

        }
        
        private void publishManifestData(String orcaSliceID, String sliceState, int numActiveWorkers){
            
            Connection connection = null;
            Channel channel = null;
            try {
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(EXCHANGE_NAME, "topic");

                String routingKey = "adamant.manifest." + orcaSliceID;
                String message = buildMessageManifestResponseData(orcaSliceID, sliceState, numActiveWorkers);

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
        
        private String buildMessageManifestResponseData(String orcaSliceID, String sliceState, int numActiveWorkers){
        
            // Build a test JSON message
            JSONObject obj = new JSONObject();
            // mandatory
            obj.put("response_orcaSliceID", orcaSliceID);
            obj.put("response_sliceStatus", sliceState);
            obj.put("response_numWorkersReady", numActiveWorkers);

            System.out.println("JSON response = \n" + obj.toJSONString());

            return obj.toJSONString();

        }
        
    }
    
    
    public static void main(String[] argv) {
        
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("gaul.isi.edu");
            factory.setPort(5671);
            factory.useSslProtocol();
            factory.setUsername("anirban");
            factory.setPassword("adamant123");
            

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

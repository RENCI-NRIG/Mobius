/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.JTextComponent;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.renci.requestmanager.AppRequestInfo;
import org.renci.requestmanager.RMConstants;
import org.renci.requestmanager.util.MessageConsole;

/**
 *
 * @author anirban
 */
public class DisplaySubscriber implements RMConstants{

  private static final String EXCHANGE_NAME = "testDisplayExchange";
  
  private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
  private static final String PREF_FILE = ".rm.properties";
  private static Properties rmProps = null;
  
  public static void main(String[] argv) throws Exception{
      
        // This populates rmProps
        processPreferences();
        
        ConnectionFactory factory = new ConnectionFactory();
        Logger logger = Logger.getLogger(DisplaySubscriber.class.getName());
        
        
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
      
      
        Connection connection = null;
        Channel channel = null;
        
        try {

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            String queueName = channel.queueDeclare().getQueue();
 
            String bindingKey = "adamant.#";             
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, true, consumer);

            JFrame frame = new JFrame("Message Console (Requests and Infrastructure Actions)");
            JPanel panel = new JPanel();  
            //JTextComponent textComponent = new JTextField("Message Log");
            JTextComponent textComponent = new JTextArea(16, 105);
            textComponent.setBackground(Color.white);
            textComponent.setSelectedTextColor(Color.red);
            JScrollPane scroll = new JScrollPane(textComponent);
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scroll.setBackground(Color.black);
            panel.add(scroll);
            MessageConsole mc = new MessageConsole(textComponent);
            //mc.redirectOut();
            mc.redirectErr(Color.green, System.err);
            mc.setMessageLines(100);
            mc.redirectOut(Color.blue, System.out);
            //mc.redirectOut(null, System.out);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);
            frame.add(panel);
            frame.setVisible(true);
            
            System.out.println("Message Console for requests and infrustructure actions");
            System.out.println("=======================================================" + "\n");
            
            
//            int i = 0;
//            while (true) {
//                System.out.println("Hello world " + i);
//                Thread.sleep(5000);
//                System.err.println("Hello world err " + i);
//                i++;
//            }

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                String routingKey = delivery.getEnvelope().getRoutingKey();

                //System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");  
                
                printMessage(routingKey, message);
                
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
  
  public static void printMessage(String routingKey, String amqpMesg){
        //System.out.println(amqpMesg);
      
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(amqpMesg);
        } catch (ParseException ex) {
            System.err.println("Exception while parsing JSON amqp message " + ex);
        }
        
        if(routingKey.startsWith("adamant.modifyrequest")){
            // modify request 
            // {"display_numResReqToMeetDeadline":4,"display_numCurrentRes":4,"display_orcaSliceID":"montage-test-1"}'
            Long requestNumtResToMeetDeadline = (Long) jsonObject.get("display_numResReqToMeetDeadline");
            int numResReqToMeetDeadline = requestNumtResToMeetDeadline.intValue();
            
            Long requestNumCurrentRes = (Long) jsonObject.get("display_numCurrentRes");
            int numCurrentRes = requestNumCurrentRes.intValue();
            
            //String sliceID = (String) jsonObject.get("display_orcaSliceID");
            
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(new Date());
            System.out.println("[ " + timeStamp + " MODIFY REQUEST ] --- " + "Number of resources to meet deadline = " + numResReqToMeetDeadline + " | " + "Number of current resources = " + numCurrentRes);
            
        }
        
        if(routingKey.startsWith("adamant.infraaction")){
            // infrastructure message
            // '{"display_inframessage":"No ORCA modify request was generated by ndllib and modify policies for montage-test-1.... Not sending any modify request to ExoGENI..."}'
            String infraMesg = (String) jsonObject.get("display_inframessage");
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(new Date());
            System.out.println("[ " + timeStamp + " INFRASTRUCTURE ACTION ] --- " + infraMesg);
        }
             
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

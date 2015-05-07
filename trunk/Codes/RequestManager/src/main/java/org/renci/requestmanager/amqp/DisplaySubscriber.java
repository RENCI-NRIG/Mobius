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
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.JTextComponent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.renci.requestmanager.AppRequestInfo;
import org.renci.requestmanager.util.MessageConsole;

/**
 *
 * @author anirban
 */
public class DisplaySubscriber {

  private static final String EXCHANGE_NAME = "testDisplayExchange";
    
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
  
  
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.renci.requestmanager.RMConstants;

/**
 *
 * @author anirban
 */
public class RequestTestPublisher implements RMConstants{
    
    private static String QUEUE_NAME = "testRequestQ";

    public static void main(String[] argv) throws Exception {
        
        Options options = new Options();
        Option requesttype    = OptionBuilder.withArgName( "requestTemplate" )
                                .hasArg()
                                .withDescription( "required request template of the form " + "condor|hadoop|mpi[_sp][_storage][_multi]" )
                                .create( "requesttype" );
        Option sliceid    = OptionBuilder.withArgName( "orcaSliceID" )
                                .hasArg()
                                .withDescription( "requested ORCA sliceID" )
                                .create( "sliceid" );
        Option numworkers    = OptionBuilder.withArgName( "numWorkers" )
                                .hasArg()
                                .withDescription( "requested number of workers" )
                                .create( "numworkers" );
        Option stitchportid    = OptionBuilder.withArgName( "stitchportID" )
                                .hasArg()
                                .withDescription( "requested stitchport identifier (optional)" )
                                .create( "stitchportid" );
        
        options.addOption( requesttype );
        options.addOption( sliceid );
        options.addOption( numworkers );
        options.addOption( stitchportid );
        
        CommandLineParser parser = new GnuParser();
        String orcaSliceID = null;
        String requestTemplateType = null;
        int numWorkers = RMConstants.CondorDefaults.getDefaultNumWorkers();
        String stitchportID = null;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, argv );
            
            if(line.hasOption("sliceid")){
                orcaSliceID = line.getOptionValue( "sliceid" );
            }
            else{
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "testrequester", options );
                System.exit(1);
            }
            
            if(line.hasOption("requesttype")){
                requestTemplateType = line.getOptionValue( "requesttype" );
            }
            else{
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "testrequester", options );
                System.exit(1);
            }
            
            if(line.hasOption("numworkers")){
                numWorkers = Integer.parseInt(line.getOptionValue( "numworkers" ));
            }
            
            if(line.hasOption("stitchportid")){
                stitchportID = line.getOptionValue( "stitchportid" );
            }
            
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Commandline parsing failed.  Reason: " + exp.getMessage() );
            System.exit(1);
        }
        
        if(argv.length < 1){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "testrequester", options );
            System.exit(1);
        }
        
        
        String message = buildMessageNewRequest(requestTemplateType, orcaSliceID, numWorkers, stitchportID);
        
        ConnectionFactory factory = new ConnectionFactory();
        
        factory.setHost("147.72.248.11");
        factory.setPort(5672);
        factory.setUsername("anirban");
        factory.setPassword("panorama123");
        factory.setVirtualHost("panorama");
        
        //factory.setHost("stewie.isi.edu");
        //factory.setPort(5671);
        //factory.useSslProtocol();
        //factory.setUsername("anirban");
        //factory.setPassword("panorama123");
        //factory.setVirtualHost("panorama");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);                
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
        System.exit(0);
        
//        try{     
//            Thread.sleep(40000); // wait for 40 seconds
//        } catch (InterruptedException ex) {
//            System.out.println("thread interrupted");
//            channel.close();
//            connection.close();
//        }
//        
        
    }
    
    private static String buildMessageNewRequest(String requestTemplateType, String orcaSliceID, int numWorkers, String stitchportID){
        
        // Build a test JSON message
        JSONObject obj = new JSONObject();
	// mandatory
        obj.put("requestType", "new");
        //obj.put("req_templateType", "condor_storage_sp_multi");
        //obj.put("req_templateType", "condor_sp_multi");
        //obj.put("req_templateType", "condor_storage");
        //obj.put("req_templateType", "condor");
        obj.put("req_templateType", requestTemplateType);
        obj.put("req_sliceID", orcaSliceID);
        obj.put("req_wfuuid", "0xcdfvgh");
        
        // optional parameters
        
        obj.put("req_numWorkers", numWorkers);
        //obj.put("req_storage", 100);
        //obj.put("req_BW", 100000000);
        //obj.put("req_imageUrl", "image url");
        //obj.put("req_imageHash", "image hash");
        //obj.put("req_imageName", "image name");
        //obj.put("req_postbootMaster", "master postboot script");
        //obj.put("req_postbootWorker", "worker postboot script");
        
        // optional link and stitchport info
        
        //obj.put("req_linkID", "0xghlkjh");
        //obj.put("req_linkBW", new Long(100000000));
        //obj.put("req_stitchportID", "Cenic@ION");
        
        if(stitchportID != null){
            obj.put("req_stitchportID", stitchportID);
            obj.put("req_linkID", "0xghlkjh");
            obj.put("req_BW", 100000000);
        }
        
        System.out.println("JSON request = \n" + obj.toJSONString());
                
        return obj.toJSONString();
        
    } 
    
}

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
    private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
    private static final String PREF_FILE = ".rm.properties";
    private static Properties rmProps = null;

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
                
        // This populates rmProps
        processPreferences();
        
        ConnectionFactory factory = new ConnectionFactory();
        Logger logger = Logger.getLogger(RequestTestPublisher.class.getName());
        
        
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

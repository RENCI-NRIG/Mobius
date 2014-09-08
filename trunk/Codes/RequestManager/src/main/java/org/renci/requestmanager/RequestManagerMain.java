/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.renci.requestmanager;

/**
 *
 * @author anirban
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.renci.requestmanager.amqp.ShadowQSubscriber;

/**
 * Hello world!
 *
 */
public class RequestManagerMain
{
    private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
    private static final String PREF_FILE = ".rm.properties";

    private static final String PUBSUB_PROP_PREFIX = "RM.pubsub";
    private static final String PUBSUB_SERVER_PROP = PUBSUB_PROP_PREFIX + ".server";
    private static final String PUBSUB_LOGIN_PROP = PUBSUB_PROP_PREFIX + ".login";
    private static final String PUBSUB_PASSWORD_PROP = PUBSUB_PROP_PREFIX + ".password";


    private static Properties rmProperties = null;

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        Logger logger = Logger.getLogger(new Object() { }.getClass().getEnclosingClass());

        // This populates rmProperties, which is neded by everybody else
        processPreferences();

        // Start the RMController timer thread
        RMController rmController = new RMController(rmProperties); // Invocation of the constructor will start the RMController thread
        
        // Start ShadowQSubscriber thread
        try {
            Thread shadowQsubscriberThread = new Thread(new ShadowQSubscriber(rmProperties));
            shadowQsubscriberThread.start();
        } catch (Exception ex) {
            logger.error("Exception while starting ShadowQSubscriber thread " + ex);
        }

        while(true){
            System.out.println("Tick...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                
            }
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
                    rmProperties = loadPropertiesFromAnyFile(prefFilePath);
                    return;
            } catch (IOException ioe) {
                    System.err.println("Unable to load global config file " + prefFilePath + ", trying local file");
            }

            prefFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + PREF_FILE;
            try {
                    rmProperties = loadPropertiesFromAnyFile(prefFilePath);
            } catch (IOException e) {
                    System.err.println("Unable to load local config file " + prefFilePath + ", exiting.");
                    System.exit(1);
            }
    }


    /**
     * loads properties from a file in the classpath
     * @param fileName
     * @return
     * @throws IOException
     */
    private static Properties loadProperties(String fileName) throws IOException {

        //File prefs = new File(fileName);
        //FileInputStream is = new FileInputStream(prefs);

        InputStream is = RequestManagerMain.class.getClassLoader().getResourceAsStream(fileName);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        Properties p = new Properties();
        p.load(bin);
        bin.close();

        return p;
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
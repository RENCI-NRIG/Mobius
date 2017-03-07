/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.renci.requestmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.renci.requestmanager.ndl.AhabManager;
import org.renci.requestmanager.orcaxmlrpc.OrcaManager;
/**
 *
 * @author anirbanmandal
 */
public class AhabManagerTest {
    
    private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
    private static final String PREF_FILE = ".rm.properties";
    private static Properties rmProps = null;
    
    //@Test
    public void testHTCondorRequest() {
            
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps); // MyClass is tested
        
        NewRequestInfo newReq = new NewRequestInfo("wfuuidxxxx", "condor", 2, -1, -1, null);
        
        String condorRequest = ahabManager.generateNewCondorRequest(newReq, "anirban.condor");

        System.out.println("Request generated: " + "\n" + condorRequest);
        
        try {
            FileUtils.writeStringToFile(new File("/tmp/condorrequest.rdf"), condorRequest);
            
            // assert statements
            //assertEquals("10 x 0 must be 0", 0, tester.multiply(10, 0));
            //assertEquals("0 x 10 must be 0", 0, tester.multiply(0, 10));
            //assertEquals("0 x 0 must be 0", 0, tester.multiply(0, 0));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
                
    }
    
    
    //@Test
    public void testHTCondorRequestMultiPoint() {
            
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps); // MyClass is tested
        
        NewRequestInfo newReq = new NewRequestInfo("wfuuidxxxx", "condor_multi", 2, -1, -1, null);
        
        String condorRequest = ahabManager.generateNewCondorRequest(newReq, "anirban.condor.multi");

        System.out.println("Request generated: " + "\n" + condorRequest);
        
        try {
            FileUtils.writeStringToFile(new File("/tmp/condorrequest.multipoint.rdf"), condorRequest);
            
            // assert statements
            //assertEquals("10 x 0 must be 0", 0, tester.multiply(10, 0));
            //assertEquals("0 x 10 must be 0", 0, tester.multiply(0, 10));
            //assertEquals("0 x 0 must be 0", 0, tester.multiply(0, 0));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
                
    }
    
    //@Test
    public void testGetNumActiveWorkersInManifest(){
        
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps);
        
        String manifest = null;        
        try {
            manifest = FileUtils.readFileToString(new File("/tmp/flukes-manifest-mp.rdf"));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int numActiveWorkersInManifest = ahabManager.getNumActiveWorkersInManifest(manifest);
        
        System.out.println("****** numActiveWorkersInManifest = " + numActiveWorkersInManifest);
        
    }
    
    
    //@Test
    public void testGetPublicIPMasterInManifest(){
        
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps);
        
        String manifest = null;        
        try {
            manifest = FileUtils.readFileToString(new File("/tmp/flukes-manifest-mp.rdf"));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        String publicIP = ahabManager.getPublicIPMasterInManifest(manifest);
        
        System.out.println("****** public IP = " + publicIP);
        Assert.assertNotNull(publicIP);
        
    }
    
    @Test
    public void testAddNewWorkersToNodeGroupInSlice(){
        
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps);
        
        String manifest = null;        
        try {
            manifest = FileUtils.readFileToString(new File("/tmp/flukes-manifest.rdf"));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        String orcaSliceID = "anirban.condor";
        String modRequest = ahabManager.addNewWorkersToNodeGroupInSlice(manifest, orcaSliceID);
        
        try {
            FileUtils.writeStringToFile(new File("/tmp/modRequest.rdf"), modRequest);
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        /* Uncomment if you want to send the modify request to ORCA
        OrcaManager orcaManager = new OrcaManager(rmProps);
        String resultModify = orcaManager.sendModifyRequestToORCA("anirban.condor", modRequest);
        
        if(resultModify == null){ // Exception
            System.out.println("Could not modify slice in ExoGENI/ORCA");
        }
        */
        
        System.out.println("****** DONE ******");
        
        
    }
    
    //@Test
    public void testGetDataPlaneIPAddressesOfComputeNodes(){
        
        processPreferences();
            
        AhabManager ahabManager = new AhabManager(rmProps);
        
        String manifest = null;        
        try {
            manifest = FileUtils.readFileToString(new File("/tmp/flukes-manifest-mp.rdf"));
        } catch (IOException ex) {
            Logger.getLogger(AhabManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        ahabManager.getDataPlaneIPAddressesOfComputeNodes(manifest);
        
        System.out.println("****** DONE ******");
        
        
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

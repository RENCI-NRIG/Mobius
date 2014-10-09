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
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author anirban
 */
public interface RMConstants {
    
    public String CondorBasicTypeName = "condor";
    public String HadoopBasicTypeName = "hadoop";
    public String MPIBasicTypeName = "mpi";
    public String SPSuffix = "_sp"; // stitch port suffix
    public String StorageSuffix = "_storage"; // storage suffix
    public String MultiSuffix = "_multi"; // *multi ones are where master and workers are in different domains
    
    public String defaultControllerUrl = "https://geni.renci.org:11443/orca/xmlrpc";
    public String defaultSPMapperUrl = "http://url.sp.mapper";
    
    public String PREFERRED_DOMAINS_STRING_NAME = "preferredDomains";
    
    public enum RequestTemplates {
        
        CONDOR_POOL(CondorBasicTypeName),
        CONDOR_POOL_SP(CondorBasicTypeName + SPSuffix),
        CONDOR_POOL_STORAGE(CondorBasicTypeName + StorageSuffix),
        CONDOR_POOL_STORAGE_SP(CondorBasicTypeName + StorageSuffix + SPSuffix),
        CONDOR_POOL_MULTI(CondorBasicTypeName + MultiSuffix), 
        CONDOR_POOL_SP_MULTI(CondorBasicTypeName + SPSuffix + MultiSuffix),
        CONDOR_POOL_STORAGE_MULTI(CondorBasicTypeName + StorageSuffix + MultiSuffix),
        CONDOR_POOL_STORAGE_SP_MULTI(CondorBasicTypeName + StorageSuffix + SPSuffix + MultiSuffix),
        HADOOP_CLUSTER(HadoopBasicTypeName),
        HADOOP_CLUSTER_STORAGE(HadoopBasicTypeName + StorageSuffix),
        MPI_CLUSTER(MPIBasicTypeName),
        MPI_CLUSTER_STORAGE(MPIBasicTypeName + StorageSuffix);

        public String name;
        RequestTemplates(String s) {
                name = s;
        }
        
    }
    
    
    public enum ComputeDomains {
        
        //BBN("BBN/GPO (Boston, MA USA) XO Rack"),
        //Duke("Duke CS (Durham, NC USA) XO Rack"),
        //UNC("UNC BEN (Chapel Hill, NC USA)"),
        //NICTA("NICTA (Sydney, Australia) XO Rack"),
        //FIU("FIU (Miami, FL USA) XO Rack"),
        //UH("UH (Houston, TX USA) XO Rack"),
        //NCSU("NCSU (Raleigh, NC USA) XO Rack"),
        //UvA("UvA (Amsterdam, The Netherlands) XO Rack"),
        
        // TODO : Add more racks here. Or, read it from a configuration file
        
        //UCD("UCD (Davis, CA USA) XO Rack"),
        RCI("RENCI (Chapel Hill, NC USA) XO Rack"),
        TAMU("TAMU (College Station, TX, USA) XO Rack");
        
        
        public String name;
        ComputeDomains(String s) {
                name = s;
        }
        
        // Way to pick a domain randomly
        //int pick = new Random().nextInt(ComputeDomains.values().length);
        //String s = ComputeDomains.values()[pick].name;
                   
        
    }
    
    public class CondorDefaults{
        
        private static final long defaultBW = 100000000 ; //100Mb/s TODO: check #0s
        private static final int defaultStorage = 100; //100GB
        private static final int defaultNumWorkers = 2; // default number of condor worker vms
        private static final String defaultImageUrl = "http://geni-images.renci.org/images/anirban/adamant/genovariant-0.2/genovariant-0.2.xml"; // defaul url to image xml file
        private static final String defaultImageHash = "4049f4ea1b92d12e0b2608a9c4cf6afca391e711"; // hash of the image xml file
        private static final String defaultImageName = "SQ-genovariant-v.2"; // default name of image
        private String defaultPostbootMaster_MultiPoint = readPostboot("default.condor.master.multipoint.postboot"); // default postboot script for master - multipoint
        private String defaultPostbootMaster_SingleDomain = readPostboot("default.condor.master.singledomain.postboot"); // default postboot script for master - single domain
        private String defaultPostbootWorker_MultiPoint = readPostboot("default.condor.worker.multipoint.postboot"); // default postboot script for workers - multipoint
        private String defaultPostbootWorker_SingleDomain = readPostboot("default.condor.worker.singledomain.postboot"); // default postboot script for workers - single domain
        private static final int defaultMaxNumWorkers = 256; // default max size of worker nodegroup
        
        private static String readStringFromFile(String filePathStr) {
            
            if(filePathStr == null || filePathStr.isEmpty()){
                System.out.println("ERROR: Can't read postboot script because file doesn't exist..");
                return null;
            }
            
            File path = new File(filePathStr);
            try {
                    FileInputStream is = new FileInputStream(path);
                    BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while((line = bin.readLine()) != null) {
                            sb.append(line);
                            // re-add line separator
                            sb.append(System.getProperty("line.separator"));
                    }

                    bin.close();

                    return sb.toString();

            } catch (IOException e) {
                    e.printStackTrace();
                    return null;
            }
            
	}
        
        private String readPostboot(String fileName){
            
            if(fileName == null || fileName.isEmpty()){
                System.out.println("ERROR: Can't read postboot script because file doesn't exist..");
                return null;
            }
            
            try {
                InputStream is = getClass().getResourceAsStream("/" + fileName);
                BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while((line = bin.readLine()) != null) {
                            sb.append(line);
                            // re-add line separator
                            sb.append(System.getProperty("line.separator"));
                    }

                    bin.close();

                    return sb.toString();
            } catch (IOException e) {
                    e.printStackTrace();
                    return null;
            }
            
        }
        
        public CondorDefaults(){
            
        }

        public String getDefaultPostbootMaster_MultiPoint() {
            return defaultPostbootMaster_MultiPoint;
        }

        public void setDefaultPostbootMaster_MultiPoint(String defaultPostbootMaster_MultiPoint) {
            this.defaultPostbootMaster_MultiPoint = defaultPostbootMaster_MultiPoint;
        }

        public String getDefaultPostbootMaster_SingleDomain() {
            return defaultPostbootMaster_SingleDomain;
        }

        public void setDefaultPostbootMaster_SingleDomain(String defaultPostbootMaster_SingleDomain) {
            this.defaultPostbootMaster_SingleDomain = defaultPostbootMaster_SingleDomain;
        }

        public String getDefaultPostbootWorker_MultiPoint() {
            return defaultPostbootWorker_MultiPoint;
        }

        public void setDefaultPostbootWorker_MultiPoint(String defaultPostbootWorker_MultiPoint) {
            this.defaultPostbootWorker_MultiPoint = defaultPostbootWorker_MultiPoint;
        }

        public String getDefaultPostbootWorker_SingleDomain() {
            return defaultPostbootWorker_SingleDomain;
        }

        public void setDefaultPostbootWorker_SingleDomain(String defaultPostbootWorker_SingleDomain) {
            this.defaultPostbootWorker_SingleDomain = defaultPostbootWorker_SingleDomain;
        }

        public static long getDefaultBW() {
            return defaultBW;
        }

        public static int getDefaultStorage() {
            return defaultStorage;
        }

        public static int getDefaultNumWorkers() {
            return defaultNumWorkers;
        }

        public static String getDefaultImageUrl() {
            return defaultImageUrl;
        }

        public static String getDefaultImageHash() {
            return defaultImageHash;
        }

        public static String getDefaultImageName() {
            return defaultImageName;
        }

        public static int getDefaultMaxNumWorkers() {
            return defaultMaxNumWorkers;
        }
        
        
        
    }
    
    // TODO Hadoop and MPI defaults
        
    
}

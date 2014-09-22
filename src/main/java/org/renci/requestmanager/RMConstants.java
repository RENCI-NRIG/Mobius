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
        
        RCI("RENCI (Chapel Hill, NC USA) XO Rack"),
        BBN("BBN/GPO (Boston, MA USA) XO Rack"),
        Duke("Duke CS (Durham, NC USA) XO Rack"),
        UNC("UNC BEN (Chapel Hill, NC USA)"),
        NICTA("NICTA (Sydney, Australia) XO Rack"),
        FIU("FIU (Miami, FL USA) XO Rack"),
        UH("UH (Houston, TX USA) XO Rack"),
        NCSU("NCSU (Raleigh, NC USA) XO Rack"),
        // TODO : Add more racks here. Or, read it from a configuration file
        UvA("UvA (Amsterdam, The Netherlands) XO Rack");
        
        
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
        private static final String defaultImageUrl = "http://geni-images.renci.org/images/standard/centos/centos6.3-v1.0.11.xml"; // defaul url to image xml file
        private static final String defaultImageHash = "776f4874420266834c3e56c8092f5ca48a180eed"; // hash of the image xml file
        private static final String defaultImageName = "Condor-default-image"; // default name of image
        private static String defaultPostbootMaster = readStringFromFile(""); // default postboot script for master
        private static String defaultPostbootWorker = readStringFromFile(""); // default postboot script for workers
        
        private static String readStringFromFile(String filePathStr) {
            
            if(filePathStr == null || filePathStr.isEmpty()){
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
                    return null;
            }
            
	}
        
        public CondorDefaults(){
            
        }

        public static String getDefaultPostbootMaster() {
            return defaultPostbootMaster;
        }

        public static void setDefaultPostbootMaster(String defaultPostbootMaster) {
            CondorDefaults.defaultPostbootMaster = defaultPostbootMaster;
        }

        public static String getDefaultPostbootWorker() {
            return defaultPostbootWorker;
        }

        public static void setDefaultPostbootWorker(String defaultPostbootWorker) {
            CondorDefaults.defaultPostbootWorker = defaultPostbootWorker;
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
        
        
    }
    
    // TODO Hadoop and MPI defaults
        
    
}

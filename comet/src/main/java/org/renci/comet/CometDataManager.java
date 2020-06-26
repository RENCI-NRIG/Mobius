package org.renci.comet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Set;

public class CometDataManager {

    public static final String ReadTokenSuffix = "read";
    public static final String WriteTokenSuffix = "write";

    public static final String PubkeysFamilyPrefix = "pubkeys";
    public static final String HostsFamilyPrefix = "hosts";

    public final static String JsonKeyVal = "val_";

    // Hostname fields
    public final static String JsonKeyHostName = "hostName";
    public final static String JsonKeyIp = "ip";

    // Key fields
    public final static String JsonKeyPublicKey = "publicKey";

    // Script fields
    public final static String JsonKeyScriptName = "scriptName";
    public final static String JsonKeyScriptBody = "scriptBody";





    private static final Logger LOGGER = LogManager.getLogger( CometDataManager.class.getName() );

    private String cometHost;
    private String cometCaCert;
    private String cometCertPwd;
    private String cometCert;


    private String getReadToken(String workflowId) {
        return workflowId + ReadTokenSuffix;
    }

    private String getWriteToken(String workflowId) {
        return workflowId + WriteTokenSuffix;
    }

    public CometDataManager(String cometHost, String cometCaCert, String cometCert, String cometCertPwd) {
        this.cometHost = cometHost;
        this.cometCaCert = cometCaCert;
        this.cometCert = cometCert;
        this.cometCertPwd = cometCertPwd;
    }

    public void createCometEntry(String workflowId, String ipAddress, Set<String> hostnames, String family) {
        LOGGER.debug("IN");
        try {
            CometInterface cometInterface = new CometInterface(cometHost, cometCaCert, cometCert, cometCertPwd);

            for (String host: hostnames) {
                JSONArray array = new JSONArray();
                JSONObject object1 = new JSONObject();
                JSONObject object2 = new JSONObject();

                // Create comet entry for pubkeysall
                object1.put(JsonKeyPublicKey, "");
                array.add(object1);
                object2.put(JsonKeyVal, array.toString());
                String tempFamily = PubkeysFamilyPrefix + family;
                cometInterface.writeScopePost(workflowId, host, getReadToken(workflowId), getWriteToken(workflowId), tempFamily, object2.toString());


                // Create comet entry for hostsall
                array.clear();
                object1.clear();
                object2.clear();
                object1.put(JsonKeyHostName, host);
                if (ipAddress != null) {
                    object1.put(JsonKeyIp, ipAddress);
                } else {
                    object1.put(JsonKeyIp, "");
                }
                array.add(object1);
                object2.put(JsonKeyVal, array.toString());
                tempFamily = HostsFamilyPrefix + family;
                cometInterface.writeScopePost(workflowId, host, getReadToken(workflowId), getWriteToken(workflowId), tempFamily, object2.toString());
            }
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while resetting COMET context for workflow: " + workflowId);
            LOGGER.error("Exception e: " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    public void createScriptEntry(String workflowId, String host, String scriptName, String scriptBody) {
        LOGGER.debug("IN");
        try {
            CometInterface cometInterface = new CometInterface(cometHost, cometCaCert, cometCert, cometCertPwd);

            JSONObject object2 = new JSONObject();
            JSONArray scripts = new JSONArray();
            JSONObject script = new JSONObject();
            script.put(JsonKeyScriptName, scriptName);
            script.put(JsonKeyScriptBody, scriptBody);

            scripts.add(script);
            object2.put(JsonKeyVal, scripts.toString());

            cometInterface.writeScopePost(workflowId, host, getReadToken(workflowId), getWriteToken(workflowId), "scripts", object2.toString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while resetting COMET context for workflow: " + workflowId);
            LOGGER.error("Exception e: " + e);
            e.printStackTrace();
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    public void resetCometContext(String workflowId) {
        LOGGER.debug("IN");
        try {
            CometInterface cometInterface = new CometInterface(cometHost, cometCaCert, cometCert, cometCertPwd);
            //cometInterface.resetFamilies(workflowId, getReadToken(workflowId), getWriteToken(workflowId));
            cometInterface.deleteFamilies(workflowId, getReadToken(workflowId), getWriteToken(workflowId));
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while resetting COMET context for workflow: " + workflowId);
            LOGGER.error("Exception e: " + e);
            e.printStackTrace();
        }
        LOGGER.debug("OUT");
    }
}

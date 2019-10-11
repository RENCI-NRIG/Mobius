package org.renci.comet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/*
 *
 * @class
 *
 * @brief This class implements an interface to interact with Comet by invoking REST APIs
 *
 *
 */
public class CometInterface {
    public final static String ReponseOk = "OK";
    public final static String JsonKeyValue = "value";
    public final static String JsonKeyVal = "val_";
    public final static String JsonKeyEnteries = "entries";
    public final static String JsonKeyEnteryKey = "key";
    public final static String JsonKeyFamily = "family";
    public final static String JsonKeyMessage = "message";

    public final static String ReadScopePath = "/readScope?contextID=%s&family=%s&Key=%s&readToken=%s";
    public final static String WriteScopePath = "/writeScope?contextID=%s&family=%s&Key=%s&readToken=%s&writeToken=%s";
    public final static String DeleteScopePath = "/deleteScope?contextID=%s&family=%s&Key=%s&readToken=%s&writeToken=%s";
    public final static String EnumerateScopePath = "/enumerateScope?contextID=%s&readToken=%s";
    public final static String EnumerateScopePathWithFamily = "/enumerateScope?contextID=%s&family=%s&readToken=%s";

    private static final Logger LOGGER = LogManager.getLogger( CometInterface.class.getName() );


    private ArrayList<String> cometHosts;
    private InputStream sslCaCert;
    private InputStream sslClientCertKS;
    private CloseableHttpClient httpClient;

    /*
     * @brief Constructor
     *
     * @param cometHosts - list of the comet headnodes
     * @param caCert - complete path of the CA certificate
     * @param clientCertKeyStore - complete path of the Client certificate keystore
     * @param clientCertKeyStorePwd - Client certificate keystore password
     *
     */
    CometInterface(String cometHosts, String caCert, String clientCertKeyStore, String clientCertKeyStorePwd) {
        this.cometHosts = new ArrayList<String>(Arrays.asList(cometHosts.split(",")));
        try {
            if(sslCaCert == null) {
                sslCaCert = new FileInputStream(caCert);
                sslClientCertKS = new FileInputStream(clientCertKeyStore);
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(sslClientCertKS, clientCertKeyStorePwd.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(ks, clientCertKeyStorePwd.toCharArray());
                SSLContext sslContext = SSLContext.getInstance("TLS");


                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(sslCaCert);
                if (certificates.isEmpty()) {
                    throw new IllegalArgumentException("expected non-empty set of trusted certificates");
                }

                KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                caKeyStore.load(null, null);

                int index = 0;
                for (Certificate certificate : certificates) {
                    String certificateAlias = "ca" + Integer.toString(index++);
                    caKeyStore.setCertificateEntry(certificateAlias, certificate);
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(caKeyStore);

                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),new SecureRandom());

                httpClient = HttpClients.custom().setSSLContext(sslContext).build();
            }
            else {
                LOGGER.debug("SSL cert is already configured or apiClient does not exist");
            }
        }
        catch (Exception e) {
            httpClient = null;
            LOGGER.error("Exception occurred while constructing CometInterface e=" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * @brief funtion invokes readScope REST API to read meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param readToken - Read Token (random generated string)
     * @param key - key
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public CometResponse readScopeGet(String contextId, String key, String readToken, String family) {
        CometResponse response = new CometResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Collections.shuffle(cometHosts);
        for (String host : cometHosts) {
            LOGGER.debug("Using cometHost=" + host);
            try {
                String url = host + ReadScopePath;
                url = String.format(url, contextId, family, key, readToken);
                HttpGet request = new HttpGet(url);
                request.addHeader("content-type", "application/json");
                request.addHeader("accept", "application/json");
                HttpResponse httpResponse = httpClient.execute(request);
                response.setStatus(httpResponse.getStatusLine().getStatusCode());
                response.setMessage(httpResponse.getStatusLine().getReasonPhrase());
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.debug("KOMAL = " + responseString);
                if(responseString != null) {
                    JSONObject object = (JSONObject) JSONValue.parse(responseString);
                    String message = (String) object.get(JsonKeyMessage);
                    if (message != null) {
                        response.setMessage((String) object.get(JsonKeyMessage));
                    }
                    Object value = object.get(JsonKeyValue);
                    if (value != null) {
                        response.setValue(value.toString());
                    }
                }
                break;
            }
            catch (Exception e) {
                LOGGER.error("Exception occurred while read: readScopeGet=" + host + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        LOGGER.debug("readScopeGet response=" + response);
        return response;
    }

    /*
     * @brief funtion invokes writeScope REST API to write meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     * @param value - Specifies Json containing metadata to be saved
     *
     * @return true for success; false otherwise
     *
     */
    public CometResponse writeScopePost(String contextId, String key, String readToken, String writeToken, String family, String value) {
        CometResponse response = new CometResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Collections.shuffle(cometHosts);
        for (String host : cometHosts) {
            LOGGER.debug("Using cometHost=" + host);
            try {
                String url = host + WriteScopePath;
                url = String.format(url, contextId, family, key, readToken, writeToken);
                HttpPost request = new HttpPost(url);
                request.addHeader("content-type", "application/json");
                request.addHeader("accept", "application/json");
                StringEntity params = new StringEntity(value);
                request.setEntity(params);

                HttpResponse httpResponse = httpClient.execute(request);
                response.setStatus(httpResponse.getStatusLine().getStatusCode());
                response.setMessage(httpResponse.getStatusLine().getReasonPhrase());
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.debug("KOMAL = " + responseString);
                if(responseString != null) {
                    JSONObject object = (JSONObject) JSONValue.parse(responseString);
                    String message = (String) object.get(JsonKeyMessage);
                    if (message != null) {
                        response.setMessage((String) object.get(JsonKeyMessage));
                    }
                    Object value1 = object.get(JsonKeyValue);
                    if (value1 != null) {
                        response.setValue(value1.toString());
                    }
                }
                break;
            }
            catch (Exception e) {
                LOGGER.error("Exception occurred while writeScopePost: cometHost=" + host + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        LOGGER.debug("writeScopePost response=" + response);
        return response;
    }


    /*
     * @brief funtion invokes deletScope REST API to delete meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public CometResponse deleteScopeDelete(String contextId, String key, String readToken, String writeToken, String family) {
        CometResponse response = new CometResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Collections.shuffle(cometHosts);
        for (String host : cometHosts) {
            LOGGER.debug("Using cometHost=" + host);
            try {
                String url = host + DeleteScopePath;
                url = String.format(url, contextId, family, key, readToken, writeToken);
                HttpDelete request = new HttpDelete(url);
                request.addHeader("content-type", "application/json");
                request.addHeader("accept", "application/json");
                HttpResponse httpResponse = httpClient.execute(request);
                response.setStatus(httpResponse.getStatusLine().getStatusCode());
                response.setMessage(httpResponse.getStatusLine().getReasonPhrase());
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.debug("KOMAL = " + responseString);
                if(responseString != null) {
                    JSONObject object = (JSONObject) JSONValue.parse(responseString);
                    String message = (String) object.get(JsonKeyMessage);
                    if (message != null) {
                        response.setMessage((String) object.get(JsonKeyMessage));
                    }
                    Object value = object.get(JsonKeyValue);
                    if (value != null) {
                        response.setValue(value.toString());
                    }
                }
                break;
            }
            catch (Exception e) {
                LOGGER.error("Exception occurred while deleteScopeDelete: cometHost=" + host + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        LOGGER.debug("deleteScopeDelete response=" + response);
        return response;
    }

    /*
     * @brief funtion invokes readScope REST API to read meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param readToken - Read Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public CometResponse enumerateScopeGet(String contextId, String readToken, String family) {
        CometResponse response = new CometResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Collections.shuffle(cometHosts);
        for (String host : cometHosts) {
            LOGGER.debug("Using cometHost=" + host);
            try {
                String url = host;
                if(family != null) {
                    url = url + EnumerateScopePathWithFamily;
                    url = String.format(url, contextId, family, readToken);
                }
                else {
                    url = url + EnumerateScopePath;
                    url = String.format(url, contextId, readToken);
                }

                HttpGet request = new HttpGet(url);
                request.addHeader("content-type", "application/json");
                request.addHeader("accept", "application/json");
                HttpResponse httpResponse = httpClient.execute(request);
                response.setStatus(httpResponse.getStatusLine().getStatusCode());
                response.setMessage(httpResponse.getStatusLine().getReasonPhrase());
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                LOGGER.debug("KOMAL = " + responseString);
                if(responseString != null) {
                    JSONObject object = (JSONObject) JSONValue.parse(responseString);
                    String message = (String) object.get(JsonKeyMessage);
                    if (message != null) {
                        response.setMessage((String) object.get(JsonKeyMessage));
                    }
                    Object value = object.get(JsonKeyValue);
                    if (value != null) {
                        response.setValue(value.toString());
                    }
                }
                break;
            }
            catch (Exception e) {
                LOGGER.error("Exception occurred while enumerateScopeGet: cometHost=" + host + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        LOGGER.debug("enumerateScopeGet response=" + response);
        return response;
    }

    /*
     * @brief funtion invokes readScope REST API to read meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param readToken - Read Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public void resetFamilies(String contextId, String readToken, String writeToken) {
        CometResponse response = enumerateScopeGet(contextId, readToken, null);
        if(response != null) {
            if (response.getStatus() != HttpStatus.SC_OK) {
                LOGGER.debug("Unable to enumerate families for context:" + contextId + " readToken:" + readToken + " writeToken:" + writeToken);
                LOGGER.debug("Status: " + response.getStatus() + "Message: " + response.getMessage());
            }
            else {
                String value = response.getValue();
                if(value != null && !value.isEmpty()) {
                    JSONObject object = (JSONObject) JSONValue.parse(value);
                    if(object != null) {
                        JSONArray enteries = (JSONArray) object.get(JsonKeyEnteries);
                        if(enteries != null) {
                            for (Object e : enteries) {
                                JSONObject entry = (JSONObject) e;
                                if(entry != null) {
                                    String key = (String)entry.get(JsonKeyEnteryKey);
                                    String family = (String) entry.get(JsonKeyFamily);
                                    if(key!= null && family != null) {
                                        LOGGER.debug("Resetting family " + family +  " for key " + key + " in context " + contextId);
                                        writeScopePost(contextId, key, readToken, writeToken, family, "{}");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            LOGGER.debug("response null");
        }
    }
}

package org.renci.mobius.controllers.sdx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.MobiusException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

public class SdxClient {
    private static final Logger LOGGER = LogManager.getLogger(SdxClient.class.getName());

    private String sdxUrl;
    private final RestTemplate rest = new RestTemplate();

    public SdxClient(String sdxUrl) {
        this.sdxUrl = sdxUrl;
    }

    public void stitchChameleon(String stitchPort, String vlan, String ip, String subnet, String destinationSite, String slicename) throws Exception {
        LOGGER.debug("IN stitchPort=" + stitchPort + " vlan=" + vlan
                + " ip=" + ip + " subnet=" + subnet + " destinationSite=" + destinationSite + " slicename=" + slicename);
        try {
            //post stitch request to SAFE
            JSONObject object = new JSONObject();
            object.put("stitchport", stitchPort);
            object.put("vlan", vlan);
            object.put("gateway", ip);
            object.put("ip", subnet);
            object.put("sdxsite", destinationSite);
            object.put("ckeyhash", slicename);
            object.put("creservid", slicename);
            String url =  sdxUrl + "sdx/stitchchameleon";

            LOGGER.debug("Sending stitch request to Sdx server: " + url + " body: " + object);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
            HttpEntity<String> requestEntity = new HttpEntity<>(object.toString(), headers);
            ResponseEntity<String> result = rest.exchange(url, HttpMethod.POST, requestEntity, String.class);

            LOGGER.debug("Unstitch Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                LOGGER.debug("Response= " + result.getBody());
            } else {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, result.toString());
            }
        }
        catch (HttpClientErrorException |HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            throw new MobiusException(e.getResponseBodyAsString());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    public void stitch(String sliceName, String siteName, String stitchingGuid, String ip, String subnet, String secret) throws Exception {
        LOGGER.debug("IN sliceName=" + sliceName + " siteName=" + siteName + " stitchingGuid=" + stitchingGuid
        + " ip=" + ip + " subnet=" + subnet + " secret=" + secret);
        try {

            JSONObject object = new JSONObject();
            object.put("sdxsite", siteName);
            object.put("gateway", ip);
            object.put("ip", subnet);
            object.put("ckeyhash", sliceName);
            object.put("cslice", sliceName);
            object.put("creservid", stitchingGuid);
            object.put("secret", secret);
            String url =  sdxUrl + "sdx/stitchrequest";

            LOGGER.debug("Sending stitch request to Sdx server: " + url + " body: " + object);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> requestEntity = new HttpEntity<>(object.toString(), headers);

            ResponseEntity<Map> result = rest.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            LOGGER.debug("Stitch Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                LOGGER.debug("Response= " + result.getBody());
                Boolean status = (Boolean) result.getBody().get("result");
                if (status) {
                    LOGGER.debug("stitch successful");
                } else {
                    LOGGER.debug("OUT");
                    throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "stitch command failed" + result.toString());
                }
            } else {
                LOGGER.debug("OUT");
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, result.toString());
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            throw new MobiusException(e.getResponseBodyAsString());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }

    public void prefix(String sliceName, String ip, String subnet) throws Exception {
        try {
            LOGGER.debug("IN sliceName=" + sliceName + " ip=" + ip + " subnet=" + subnet);

            JSONObject object =  new JSONObject();
            object.put("dest", subnet);
            object.put("gateway", ip);
            if(sliceName != null) {
                object.put("customer", sliceName);
            }
            String url =  sdxUrl + "sdx/notifyprefix";

            LOGGER.debug("Sending prefix request to Sdx server: " + url + " body: " + object);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> requestEntity = new HttpEntity<>(object.toString(), headers);

            ResponseEntity<Map> result = rest.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            LOGGER.debug("prefix Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                LOGGER.debug("Response= " + result.getBody());
                Boolean status = (Boolean) result.getBody().get("result");
                if(status) {
                    LOGGER.debug("prefix successful");
                }
                else {
                    throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "prefix command failed" + result.toString());
                }
            }
            else {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, result.toString());
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            throw new MobiusException(e.getResponseBodyAsString());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }
    public void connect(String sourceSubnet, String targetSubnet, String bandwidth) throws Exception {
        LOGGER.debug("IN sourceSubnet=" + sourceSubnet + " targetSubnet=" +
                targetSubnet + " bandwidth=" + bandwidth);
        try {
            JSONObject object = new JSONObject();
            object.put("self_prefix", sourceSubnet);
            object.put("target_prefix", targetSubnet);
            object.put("bandwidth", bandwidth);
            String url =  sdxUrl + "sdx/connectionrequest";

            LOGGER.debug("Sending connect request to Sdx server: " + url + " body: " + object);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

            HttpEntity<String> requestEntity = new HttpEntity<>(object.toString(), headers);

            ResponseEntity<String> result = rest.exchange(url, HttpMethod.POST,
                    requestEntity, String.class);
            LOGGER.debug("connection Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                LOGGER.debug("Response= " + result.getBody());
            } else {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, result.toString());
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            throw new MobiusException(e.getResponseBodyAsString());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }
    public void unstitch(String sliceName, String stitchingGuid) throws Exception {
        LOGGER.debug("IN sliceName=" + sliceName + " stitchingGuid=" + stitchingGuid);
        try {

            JSONObject object = new JSONObject();
            object.put("ckeyhash", sliceName);
            object.put("cslice", sliceName);
            object.put("creservid", stitchingGuid);

            String url =  sdxUrl + "sdx/undostitch";

            LOGGER.debug("Sending unstitch request to Sdx server: " + url + " body: " + object);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
            HttpEntity<String> requestEntity = new HttpEntity<>(object.toString(), headers);

            ResponseEntity<String> result = rest.exchange(url, HttpMethod.POST, requestEntity, String.class);
            LOGGER.debug("Unstitch Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                LOGGER.debug("Response= " + result.getBody());
            } else {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, result.toString());
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            throw new MobiusException(e.getResponseBodyAsString());
        }
        finally {
            LOGGER.debug("OUT");
        }
    }
}

package org.renci.mobius.controllers.mos;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;


public class OsSsoAuth {
    private String accesEndPoint;
    private String federatedIdentityProvider;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;
    private String scope;

    private final RestTemplate rest = new RestTemplate();
    private static final Logger LOGGER = LogManager.getLogger(OsSsoAuth.class.getName());

    private final static String X_Subject_Token = "X-Subject-Token";
    private static final String AUTH_DOCUMENT = "grant_type=password" +
            "&" +
            "username=%s" +
            "&" +
            "password=%s" +
            "&" +
            "scope=%s";

    /*
     * @brief constructor
     *
     * @param authUrl - auth url for chameleon
     * @parm username - chameleon user name
     * @param password - chameleon user password
     * @param userDomain - chameleon user domain
     * @param projectName - chameleon project Name
     * @param projectDomain - chameleon project Domain
     */
    public OsSsoAuth(String accesEndPoint, String federatedIdentityProvider, String clientId,
                     String clientSecret, String userName, String password, String scope) {
        this.accesEndPoint = accesEndPoint;
        this.federatedIdentityProvider = federatedIdentityProvider;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userName = userName;
        this.password = password;
        this.scope = scope;
    }

    /*
     * @brief function to generate open id tokens to be used for generating federated identity token for a user
     *
     * @return accessToken
     *
     * @throws exception in case of error
     */
    private String auth() throws Exception {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + authentication);

            LOGGER.debug("accesEndPoint=" + accesEndPoint);
            String content = String.format(AUTH_DOCUMENT, userName, password, scope);
            LOGGER.debug("Auth Content: " + content);
            HttpEntity<String> requestEntity = new HttpEntity<>(content, headers);

            ResponseEntity<Map> result = rest.exchange(accesEndPoint, HttpMethod.POST, requestEntity, Map.class);

            LOGGER.debug("Auth Token Post Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                result.getStatusCode() == HttpStatus.ACCEPTED ||
                result.getStatusCode() == HttpStatus.CREATED) {

                String accessToken = (String) result.getBody().get("access_token");

                if (accessToken == null) {
                    throw new SsoAuthException("Failed to get access token");
                }

                LOGGER.debug("AccessToken: " + accessToken);
                return accessToken;
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new SsoAuthException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while getting access token e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());

            e.printStackTrace();
            throw new SsoAuthException("failed to get access token e=" + e.getMessage());

        }
        return null;
    }

    /*
     * @brief function to generate federated identity token for a user to be used for invoking openstack APIs
     *
     * @return federated identity token
     *
     * @throws exception in case of error
     */
    public String federatedToken() throws Exception {
        String retVal = null;
        try {
            String accessToken = auth();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> result = rest.exchange(federatedIdentityProvider, HttpMethod.POST, requestEntity, Map.class);
            LOGGER.debug("Federated Auth Token Post Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {
                HttpHeaders resultHeaders = result.getHeaders();
                retVal = resultHeaders.get(X_Subject_Token).get(0);
                Map<String, Object> tokenObject = (Map<String, Object>) result.getBody().get("token");

                if (tokenObject == null) {
                    throw new SsoAuthException("Failed to get federated identity token");
                }
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new SsoAuthException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while getting federated identity token e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new SsoAuthException("failed to federated identity token e=" + e.getMessage());
        }
        return retVal;
    }

}
package org.renci.mobius.controllers.chameleon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.util.Pair;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.MobiusException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OsReservationApi {
    private String authUrl;
    private String username;
    private String password;
    private String userDomain;
    private String projectName;
    private String projectDomain;
    private String reservationUrl;

    private final RestTemplate rest = new RestTemplate();
    private final String tokenUrl = "/auth/tokens";
    private final String leaseUrl = "/leases";
    private final static String X_Subject_Token = "X-Subject-Token";
    private static final Logger LOGGER = Logger.getLogger(OsReservationApi.class.getName());

    private static final String AUTH_DOCUMENT = "{\n" +
            "     \"auth\": {\n" +
            "         \"identity\": {\n" +
            "             \"methods\": [\"password\"],\n" +
            "             \"password\": {\n" +
            "                 \"user\": {\n" +
            "                     \"domain\": {\n" +
            "                         \"name\": \"%s\"\n" +
            "                         },\n" +
            "                     \"name\": \"%s\",\n" +
            "                     \"password\": \"%s\"\n" +
            "                 }\n" +
            "             }\n" +
            "         },\n" +
            "         \"scope\": {\n" +
            "             \"project\": {\n" +
            "                 \"domain\": {\n" +
            "                     \"name\": \"%s\"\n" +
            "                 },\n" +
            "                 \"name\":  \"%s\"\n" +
            "             }\n" +
            "         }\n" +
            "     }\n" +
            " }";

    public OsReservationApi(String authUrl, String username, String password,
                            String userDomain, String projectName, String projectDomain) {
        this.authUrl = authUrl;
        this.username = username;
        this.password = password;
        this.userDomain = userDomain;
        this.projectName = projectName;
        this.projectDomain = projectDomain;
        reservationUrl = null;
    }

    private String auth(String region) throws Exception {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> requestEntity = new HttpEntity<>(String.format(AUTH_DOCUMENT, userDomain, username,
                    password, projectDomain, projectName), headers);

            ResponseEntity<Map> result = rest.exchange(authUrl + tokenUrl, HttpMethod.POST, requestEntity, Map.class);
            LOGGER.debug("Auth Token Post Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK ||
                    result.getStatusCode() == HttpStatus.ACCEPTED ||
                    result.getStatusCode() == HttpStatus.CREATED) {

                HttpHeaders resultHeaders = result.getHeaders();
                String token = resultHeaders.get(X_Subject_Token).get(0);
                Map<String, Object> tokenObject = (Map<String, Object>) result.getBody().get("token");

                if (tokenObject == null) {
                    throw new MobiusException("Failed to get token");
                }

                List<Map<String, Object>> catalog = (List<Map<String, Object>>) tokenObject.get("catalog");

                if (catalog == null) {
                    throw new MobiusException("Failed to get catalog");
                }

                Map<String, Object> reservationEndPoint = null;
                for (Map<String, Object> endpoint : catalog) {
                    String name = (String) endpoint.get("name");
                    if (name.compareToIgnoreCase("blazar") == 0) {
                        LOGGER.debug("endpoint=" + endpoint);
                        reservationEndPoint = endpoint;
                        break;
                    }
                }
                List<Map<String, Object>> endPoints = (List<Map<String, Object>>) reservationEndPoint.get("endpoints");
                if (endPoints == null) {
                    throw new MobiusException("Failed to get endPoints");
                }
                for (Map<String, Object> endpoint : endPoints) {
                    String endPointRegion = (String) endpoint.get("region");
                    String endPointInterface = (String) endpoint.get("interface");
                    if (endPointRegion.compareToIgnoreCase(region) == 0 &&
                            endPointInterface.compareToIgnoreCase("public") == 0) {
                        LOGGER.debug(endpoint);
                        reservationUrl = (String) endpoint.get("url");
                        LOGGER.debug("Reservation Url = " + reservationUrl);
                        break;
                    }
                }

                return token;
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new MobiusException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while getting tokens e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());

            e.printStackTrace();
            throw new MobiusException("failed to get token");

        }
        return null;
    }

    private String leaseExists(String region, String name, String token) throws Exception {
        String retVal = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> result = rest.exchange(reservationUrl + leaseUrl, HttpMethod.GET, requestEntity, String.class);
            LOGGER.debug("Get Lease List Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                String jsonStr = result.getBody();
                JsonNode rootNode = mapper.readTree(jsonStr);
                List<Map<String, Object>> leases = null;

                if (rootNode instanceof JsonNode) {

                    LOGGER.debug("Get Leases result is JsonNode");
                    leases = mapper.readValue(rootNode.get("leases").toString(), List.class);
                } else if (rootNode instanceof ArrayNode) {

                    LOGGER.debug("Get Leases result is ArrayNode");
                    leases = mapper.readValue(rootNode.toString(), List.class);
                }
                for (Map<String, Object> l : leases) {
                    String leaseName = (String) l.get("name");
                    if (leaseName.compareToIgnoreCase(name) == 0) {

                        LOGGER.debug("Lease already exists");
                        retVal = (String) l.get("id");
                        break;
                    }
                }
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new MobiusException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while getting leases e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new MobiusException("failed to get leases");
        }
        return retVal;
    }

    private Map<String, Object> getLease(String region, String id, String token) throws Exception {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> result = rest.exchange(reservationUrl + leaseUrl + "/" + id, HttpMethod.GET,
                    requestEntity, Map.class);
            LOGGER.debug("Get Lease Response Status Code=" + result.getStatusCode());

            if (result.getStatusCode() == HttpStatus.OK) {
                LOGGER.debug("Successfully retrieved lease");
                Map<String, Object> lease = (Map<String, Object>) result.getBody().get("lease");
                return lease;
            } else {
                throw new MobiusException("failed to delete lease");
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new MobiusException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while retrieving lease e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new MobiusException("failed to delete lease");
        }
    }

    public Pair<String, Integer> constructHostLeaseRequest(String name, String startTime, String endTime,
                                             Map<String, Integer> nodeTypeCountMap) {
        if(name == null || startTime == null || endTime == null ||
                nodeTypeCountMap == null || nodeTypeCountMap.size() == 0) {
            return null;
        }
        JSONObject request = new JSONObject();
        request.put("name", name);
        request.put("start_date", startTime);
        request.put("end_date", endTime);
        JSONArray events = new JSONArray();
        request.put("events", events);
        JSONArray reservations  = new JSONArray();
        Integer totalComputeNodes = 0;
        for (Map.Entry<String, Integer> e : nodeTypeCountMap.entrySet()) {
            JSONObject r = new JSONObject();
            r.put("resource_type", "physical:host");
            r.put("min", e.getValue());
            r.put("max", e.getValue());
            r.put("resource_properties", "");
            r.put("before_end", "default");
            r.put("hypervisor_properties", "[\"==\",\"$node_type\",\"" + e.getKey() + "\"]");
            reservations.add(r);
            totalComputeNodes += e.getValue();
        }
        request.put("reservations", reservations);
        return new Pair<String, Integer>(request.toString(), totalComputeNodes);
    }

    public Pair<String, String> createComputeLease(String region, String name, String request,
                                                   int timeoutInSeconds) throws Exception {
        LOGGER.debug("Sending request=" + request);
        String reservationId = null, leaseId = null;
        try {
            String token = auth(region);
            if (token != null) {
                leaseId = leaseExists(region, name, token);

                if (leaseId == null) {
                    if(request == null) {
                        throw new MobiusException("Failed to construct lease request");
                    }
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Auth-Token", token);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                    HttpEntity<String> requestEntity = new HttpEntity<>(request, headers);

                    ResponseEntity<Map> result = rest.exchange(reservationUrl + leaseUrl, HttpMethod.POST,
                            requestEntity, Map.class);

                    LOGGER.debug("Create Lease Response Status Code=" + result.getStatusCode());

                    if (result.getStatusCode() == HttpStatus.OK ||
                            result.getStatusCode() == HttpStatus.ACCEPTED ||
                            result.getStatusCode() == HttpStatus.CREATED) {
                        LOGGER.debug("Response= " + result.getBody());
                        Map<String, Object> lease = (Map<String, Object>) result.getBody().get("lease");
                        if (lease != null) {
                            leaseId = (String) lease.get("id");
                            List<Map<String, Object>> reservation = (List<Map<String, Object>>) lease.get("reservations");
                            if (reservation != null && reservation.size() != 0) {
                                LOGGER.debug("Reservation=" + reservation);
                                reservationId = (String) reservation.get(0).get("id");
                            }
                        }
                    }
                }
                String status = "Failed";
                while(status.compareToIgnoreCase("active") != 0 && timeoutInSeconds != 0) {
                    Map<String, Object> lease = getLease(region, leaseId, token);
                    if (lease != null) {
                        List<Map<String, Object>> reservation = (List<Map<String, Object>>) lease.get("reservations");
                        if (reservation != null && reservation.size() != 0) {
                            LOGGER.debug("Reservation=" + reservation);
                            status = (String) reservation.get(0).get("status");
                        }
                    }
                    Thread.sleep(1000);
                    --timeoutInSeconds;
                }

            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new MobiusException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while create lease e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new MobiusException("failed to create leases");
        }
        return new Pair<>(leaseId, reservationId);
    }

    public void deleteLease(String region, String id) throws Exception {
        try {
            String token = auth(region);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> result = rest.exchange(reservationUrl + leaseUrl + "/" + id, HttpMethod.DELETE,
                    requestEntity, String.class);
            LOGGER.debug("Delete Lease Response Status Code=" + result.getStatusCode());
            if (result.getStatusCode() == HttpStatus.OK || result.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOGGER.debug("Successfully deleted lease");
            } else {
                throw new MobiusException("failed to delete lease");
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new MobiusException(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occured while delete lease e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new MobiusException("failed to delete lease");
        }
    }

}
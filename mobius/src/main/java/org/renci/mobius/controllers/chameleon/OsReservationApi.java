package org.renci.mobius.controllers.chameleon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.MobiusException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.util.Pair;

import java.util.*;
/*
 * @brief class representing interface to perform CRUD operations for reservations
 * @author kthare10
 */
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

    /*
     * @brief function to generate auth tokens to be used for openstack rest apis for reservations
     *
     * @param region - chameleon region
     *
     * @return token id
     *
     * @throws exception in case of error
     */
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

    /*
     * @brief function to check if a lease with a name already exists
     *
     * @param name - lease name
     * @param token - token id
     *
     * @return lease id
     *
     * @throws exception in case of error
     */
    private String leaseExists(String name, String token) throws Exception {
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

    /*
     * @brief function to fetch a lease with lease id
     *
     * @param name - lease id
     * @param token - token id
     *
     * @return map containing lease object
     *
     * @throws exception in case of error
     */
    private Map<String, Object> getLease(String id, String token) throws Exception {
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

    /*
     * @brief Construct Lease Request body
     *
     * @param name - name for the lease
     * @param startTime - lease start time
     * @param endTime - lease end time
     * @param nodeTypeCountMap - mapping of nodeType to nodeCount for each type
     *
     * @return returns lease request body
     */
    public String buildLeaseRequest(String name, String startTime, String endTime,
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
        for (Map.Entry<String, Integer> e : nodeTypeCountMap.entrySet()) {
            JSONObject r = new JSONObject();
            r.put("resource_type", "physical:host");
            r.put("min", e.getValue());
            r.put("max", e.getValue());
            r.put("resource_properties", "");
            r.put("before_end", "default");
            r.put("hypervisor_properties", "[\"==\",\"$node_type\",\"" + e.getKey() + "\"]");
            reservations.add(r);
        }
        request.put("reservations", reservations);
        return request.toString();
    }

    /*
     * @brief function to provision a reservation and wait for it to become active; incase reservation
     *        does not become active; it is deleted and a failure is returned
     *
     * @param region - region for which reservation to be created
     * @param name - reservation name
     * @param request - reservation request
     * @param timeoutInSeconds - timeout in seconds for which to wait for reservation to become active
     *
     * @return return a pair of leaseId and map of <reservation id, node count>
     *
     * @throws execption in case of failure
     *
     */
    public Pair<String, Map<String, Integer>> createLease(String region, String name, String request,
                                                   int timeoutInSeconds) throws Exception {

        if(region == null || name == null || request == null) {
            throw new MobiusException("Failed to construct lease request; invalid input params");
        }
        Map<String, Integer> reservationIds = new HashMap<>();
        String leaseId = null;
        try {
            String token = auth(region);
            if (token != null) {
                leaseId = leaseExists(name, token);

                if (leaseId == null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Auth-Token", token);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                    HttpEntity<String> requestEntity = new HttpEntity<>(request, headers);

                    LOGGER.debug("Sending request=" + request);

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
                            List<Map<String, Object>> reservations = (List<Map<String, Object>>) lease.get("reservations");
                            if (reservations != null) {
                                for(Map<String, Object> r : reservations) {
                                    LOGGER.debug("Reservation=" + r);
                                    reservationIds.put((String) r.get("id"), (Integer)r.get("max"));
                                }
                            }
                        }
                    }
                }
                String status = "Failed";
                int reservationCount = reservationIds.size();
                LOGGER.debug("Reservation count: " + reservationCount);
                while(reservationCount != 0 && timeoutInSeconds != 0) {
                    Map<String, Object> lease = getLease(leaseId, token);
                    int activeCount = 0;
                    if (lease != null) {
                        List<Map<String, Object>> reservations = (List<Map<String, Object>>) lease.get("reservations");
                        if (reservations != null) {
                            for(Map<String, Object> r : reservations) {
                                LOGGER.debug("Reservation=" + r);
                                status = (String)r.get("status");
                                if(status.compareToIgnoreCase("active") == 0) {
                                    activeCount++;
                                }
                                else if(status.compareToIgnoreCase("error") == 0) {
                                    LOGGER.debug("Lease in error state");
                                    break;
                                }
                            }
                        }
                    }
                    reservationCount -= activeCount;
                    LOGGER.debug("Reservation count: " + reservationCount);
                    Thread.sleep(1000);
                    --timeoutInSeconds;
                }
                if(reservationCount != 0) {
                    LOGGER.debug("Lease did not transition to Active status in " + timeoutInSeconds + " seconds!");
                    deleteLease(region, leaseId);
                    throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lease did not transition to Active status in " + timeoutInSeconds + " seconds!");
                }
            }
        }
        catch (HttpClientErrorException|HttpServerErrorException e) {
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
        return Pair.of(leaseId, reservationIds);
    }

    /*
     * @brief function to provision delete a reservation
     *
     * @param region - region for which reservation to be created
     * @param id - lease id
     *
     * @throws execption in case of failure
     *
     */
    public void deleteLease(String region, String id) throws Exception {
        try {
            if(region == null || id == null) {
                return;
            }
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

    /*
     * @brief function to update a reservation
     *
     * @param region - region for which reservation to be created
     * @param id - reservation id
     * @param leaseEndTime - new lease end time
     *
     * @throws execption in case of failure
     *
     */
    public void updateLease(String region, String id, String leaseEndTime) throws Exception {
        try {
            if(region == null || id == null || leaseEndTime == null) {
                throw new MobiusException(HttpStatus.INTERNAL_SERVER_ERROR, "region, lease id or endtime null");
            }
            String token = auth(region);
            if(token != null) {
                Map<String, Object> lease = getLease(id, token);
                if(lease != null) {
                    LOGGER.debug("lease=" + lease);
                    JSONArray updatedReservations  = null;
                    List<Map<String, Object>> reservations = (List<Map<String, Object>>) lease.get("reservations");
                    if(reservations != null) {
                        updatedReservations  = new JSONArray();
                        for (Map<String, Object> r : reservations) {
                            JSONObject newReservation = new JSONObject();
                            newReservation.put("id", (String) r.get("id"));
                            newReservation.put("max", (Integer) r.get("max"));
                            newReservation.put("hypervisor_properties", (String) r.get("hypervisor_properties"));
                            updatedReservations.add(newReservation);
                        }
                    }
                    JSONObject request = new JSONObject();
                    request.put("name", (String)lease.get("name"));
                    request.put("end_date", leaseEndTime);
                    if(updatedReservations != null) {
                        request.put("reservations", updatedReservations);
                    }


                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Auth-Token", token);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                    HttpEntity<String> requestEntity = new HttpEntity<>(request.toString(), headers);

                    LOGGER.debug("Sending request=" + request.toString());

                    ResponseEntity<Map> result = rest.exchange(reservationUrl + leaseUrl + "/" + id, HttpMethod.PUT,
                            requestEntity, Map.class);

                    LOGGER.debug("Update Lease Response Status Code=" + result.getStatusCode());

                    if (result.getStatusCode() == HttpStatus.OK ||
                            result.getStatusCode() == HttpStatus.ACCEPTED ||
                            result.getStatusCode() == HttpStatus.CREATED) {
                        LOGGER.debug("Response= " + result.getBody());
                    }
                    else {
                        LOGGER.debug("Update lease failed; Response= " + result.getBody());
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
            LOGGER.error("Exception occured while update lease e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new MobiusException("failed to update lease");
        }
    }


}
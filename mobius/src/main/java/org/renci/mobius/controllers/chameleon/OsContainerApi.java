package org.renci.mobius.controllers.chameleon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OsContainerApi extends OsReservationApi {
    private final String containersUrl = "https://chi.edge.chameleoncloud.org:9517/v1/containers/";
    private final String CONTAINER_DOC_PORTS = "{\"name\":\"%s\",\"image\":\"%s\"," +
            "\"image_driver\":\"docker\",\"command\":\"%s\",\"run\":true,\"auto_heal\":false,\"mounts\":%s," +
            "\"security_groups\":[\"default\"],\"workdir\":\"%s\",\"interactive\":true,\"hints\":{\"reservation\":\"%s\"}," +
            "\"environment\":%s,\"labels\":%s,\"exposed_ports\":%s,\"nets\":[{\"network\":\"%s\"}]}";

    private final String CONTAINER_DOC = "{\"name\":\"%s\",\"image\":\"%s\"," +
            "\"image_driver\":\"docker\",\"command\":\"%s\",\"run\":true,\"auto_heal\":false,\"mounts\":%s," +
            "\"security_groups\":[\"default\"],\"workdir\":\"%s\",\"interactive\":true,\"hints\":{\"reservation\":\"%s\"}," +
            "\"environment\":%s,\"labels\":%s,\"nets\":[{\"network\":\"%s\"}]}";

    /*
     * @brief constructor
     *
     * @param authUrl - auth url for chameleon
     * @parm federatedToken - chameleon federatedToken
     * @param userDomain - chameleon user domain
     * @param projectName - chameleon project Name
     * @param projectDomain - chameleon project Domain
     */
    public OsContainerApi(String authUrl, String federatedToken,
                          String userDomain, String projectId, String projectDomain) {
        super(authUrl, federatedToken, userDomain, projectId, projectDomain);
    }

    public OsContainerApi(String authUrl, String username, String password,
                            String userDomain, String projectName, String projectDomain) {
        super(authUrl, username, password, userDomain, projectDomain, projectDomain);
    }

    private String buildObject(Map<String, String> keyValueMap) {
        JSONObject object = new JSONObject();
        if(keyValueMap != null) {
            for (Map.Entry<String, String> e : keyValueMap.entrySet()) {
                object.put(e.getKey(), e.getValue());
            }
        }
        return object.toString();
    }

    private String buildArray(List<Map<String, String>> keyValueMapList) {
        JSONArray array = new JSONArray();
        if(keyValueMapList != null) {
            for(Map<String, String> elem : keyValueMapList) {
                JSONObject object = new JSONObject();
                for (Map.Entry<String, String> e : elem.entrySet()) {
                    object.put(e.getKey(), e.getValue());
                }
                array.add(object);
            }
        }
        return array.toJSONString();
    }

    private String buildObject(List<String> stringList) {
        JSONObject object = new JSONObject();
        if(stringList != null) {
            for(String elem: stringList) {
                JSONObject emptyObject = new JSONObject();
                object.put(elem, emptyObject);
            }
        }
        return object.toJSONString();
    }

    public Map<String, Object> create(String region, String name, String image, String command, List<Map<String, String>> mounts,
                                      String workDirectory, String reservationId, Map<String, String> environment,
                                      Map<String, String> labels, List<String> exposedPorts, String networkId) throws Exception {
        Map<String, Object> result_map = null;
        try {
            if(region == null || name == null || image == null || networkId == null) {
                throw new Exception(String.format("Missing mandatory parameters! region: %s name: %s image: %s networkId: %s",
                        region, name, image, networkId));
            }
            String request = null;
            if (exposedPorts != null && exposedPorts.size() > 0) {
                request = String.format(CONTAINER_DOC_PORTS, name, image, command, buildArray(mounts), workDirectory,
                        reservationId, buildObject(environment), buildObject(labels), buildObject(exposedPorts), networkId);
            }
            else {
                request = String.format(CONTAINER_DOC, name, image, command, buildArray(mounts), workDirectory,
                        reservationId, buildObject(environment), buildObject(labels), networkId);
            }
            String token = auth(region);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> result = rest.exchange(containersUrl, HttpMethod.POST,
                    requestEntity, Map.class);
            LOGGER.debug("Create Container Response Status Code=" + result.getStatusCode());
            LOGGER.debug("Create Container Response Body=" + result.getBody());
            result_map = result.getBody();
            if (result.getStatusCode() == HttpStatus.OK || result.getStatusCode() == HttpStatus.ACCEPTED) {
                LOGGER.debug("Successfully created container");
            } else {
                throw new Exception(String.format("Failed to create container: %s", result.toString()));
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new Exception(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred while creating container e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new Exception("failed to create container e=" + e.getMessage());
        }
        return result_map;
    }

    public Map<String, Object> delete(String region, String uuid) throws Exception {
        Map<String, Object> result_map = null;
        try {
            if(region == null || uuid == null) {
                throw new Exception(String.format("Missing mandatory parameters! region: %s uuid: %s ",
                        region, uuid));
            }
            String token = auth(region);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> result = rest.exchange(containersUrl + "/" + uuid, HttpMethod.DELETE,
                    requestEntity, Map.class);
            LOGGER.debug("Delete Container Response Status Code=" + result.getStatusCode());
            LOGGER.debug("Delete Container Response Body=" + result.getBody());
            result_map = result.getBody();
            if (result.getStatusCode() == HttpStatus.OK || result.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOGGER.debug("Successfully deleted container");
            } else {
                throw new Exception(String.format("Failed to delete container: %s", result.toString()));
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new Exception(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred while deleting container e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new Exception("failed to delete container e=" + e.getMessage());
        }
        return result_map;
    }

    public Map<String, Object> get(String region, String uuid) throws Exception {
        Map<String, Object> result_map = null;
        try {
            if(region == null || uuid == null) {
                throw new Exception(String.format("Missing mandatory parameters! region: %s uuid: %s ",
                        region, uuid));
            }
            String token = auth(region);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> result = rest.exchange(containersUrl + "/" + uuid, HttpMethod.GET,
                    requestEntity, Map.class);
            LOGGER.debug("Get Container Response Status Code=" + result.getStatusCode());
            LOGGER.debug("Get Container Response Body=" + result.getBody());
            result_map = result.getBody();
            if (result.getStatusCode() == HttpStatus.OK || result.getStatusCode() == HttpStatus.NO_CONTENT) {
                LOGGER.debug("Successfully fetched container");
            } else {
                throw new Exception(String.format("Failed to fetch container: %s", result.toString()));
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new Exception(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred while fetching container e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new Exception("failed to fetch container e=" + e.getMessage());
        }
        return result_map;
    }
}
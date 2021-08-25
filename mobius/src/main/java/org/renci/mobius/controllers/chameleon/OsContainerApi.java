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
    private final String DEFAULT_IMAGE_DRIVER = "docker";

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

    private JSONObject buildObject(Map<String, String> keyValueMap) {
        JSONObject object = new JSONObject();
        if(keyValueMap != null) {
            for (Map.Entry<String, String> e : keyValueMap.entrySet()) {
                object.put(e.getKey(), e.getValue());
            }
        }
        return object;
    }

    private JSONArray buildArray(List<Map<String, String>> keyValueMapList) {
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
        return array;
    }

    private JSONObject buildObject(List<String> stringList) {
        JSONObject object = new JSONObject();
        if(stringList != null) {
            for(String elem: stringList) {
                JSONObject emptyObject = new JSONObject();
                object.put(elem, emptyObject);
            }
        }
        return object;
    }

    private String constructBody(String name, String image, String imageDriver, Map<String, String> environment,
                                 List<String> exposedPorts, String runtime, String networkId, String reservationId,
                                 String command, String workDirectory, Map<String, String> labels,
                                 List<Map<String, String>> mounts) throws Exception {
        //{'name': 'my-first-container', 'image': 'taoyou/iperf3-alpine:latest', 'image_driver': 'docker', 'nets': [{'network': '9471db15-b33b-4a95-8e2b-1b7f69c0be7c'}],
        // 'exposed_ports': {'5201': {}}, 'environment': None, 'runtime': None, 'hints': {'reservation': 'bbab83be-8283-4f73-90da-9f8bb902957a'}, 'command': ['iperf3', '-s', '-p', '5201']}
        if (name == null || image == null || networkId == null || reservationId == null) {
            throw new Exception(String.format("Missing mandatory parameters! name: %s image: %s networkId: %s",
                    name, image, networkId));
        }
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("image", image);
        body.put("image_driver", DEFAULT_IMAGE_DRIVER);
        if(imageDriver != null) {
            body.put("image_driver", imageDriver);
        }
        JSONArray networks = new JSONArray();
        JSONObject network = new JSONObject();
        network.put("network", networkId);
        networks.add(network);
        body.put("nets", networks);
        if (exposedPorts != null) {
            JSONObject ports = buildObject(exposedPorts);
            body.put("exposed_ports", ports);
        }
        if (environment != null) {
            JSONObject env = buildObject(environment);
            body.put("environment", env);
        }
        if (runtime != null) {
            body.put("runtime", runtime);
        }
        JSONObject res = new JSONObject();
        res.put("reservation", reservationId);
        body.put("hints", res);
        if (command != null){
            body.put("command", command);
        }
        if(workDirectory != null) {
            body.put("workdir", workDirectory);
        }
        if(labels != null) {
            body.put("labels", buildObject(labels));
        }
        if(mounts != null) {
            body.put("mounts", buildArray(mounts));
        }
        return body.toJSONString();
    }

    public Map<String, Object> create(String region, String name, String image, String imageDriver,
                                       Map<String, String> environment, List<String> exposedPorts, String runtime,
                                       String networkId, String reservationId, String command, String workDirectory,
                                       Map<String, String> labels, List<Map<String, String>> mounts) throws Exception {
        Map<String, Object> result_map = null;
        try {
            String body = constructBody(name, image, imageDriver, environment, exposedPorts, runtime, networkId,
                    reservationId, command, workDirectory, labels, mounts);
            String token = auth(region);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);

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
            // Wait for upto 30 minutes for a container to be created
            String uuid = (String) result_map.get("uuid");
            Integer timeoutInSeconds = 60 * 30;
            while(timeoutInSeconds != 0) {
                Map<String, Object> container = get(region, uuid);
                if (container != null) {
                    String status = (String) container.get("status");
                    LOGGER.debug("Container " + uuid + " status=" + status);
                    if(status.compareToIgnoreCase("created") == 0) {
                        LOGGER.debug("Container " + uuid + " created successfully, starting the container!");
                        start(region, uuid);
                        break;
                    }
                    else if(status.compareToIgnoreCase("error") == 0) {
                        LOGGER.debug("Container " + uuid + " failed to create!");
                    }
                    Thread.sleep(1000);
                    --timeoutInSeconds;
                    LOGGER.debug("Waiting for the container= " + uuid + " to be created");
                }
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

    public Map<String, Object> start(String region, String uuid) throws Exception {
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

            ResponseEntity<Map> result = rest.exchange(containersUrl + "/" + uuid + "/start", HttpMethod.POST,
                    requestEntity, Map.class);
            LOGGER.debug("Start Container Response Status Code=" + result.getStatusCode());
            LOGGER.debug("Start Container Response Body=" + result.getBody());
            result_map = result.getBody();
            if (result.getStatusCode() == HttpStatus.OK || result.getStatusCode() == HttpStatus.ACCEPTED) {
                LOGGER.debug("Successfully started container");
            } else {
                throw new Exception(String.format("Failed to start container: %s", result.toString()));
            }
        }
        catch (HttpClientErrorException e) {
            LOGGER.error("HTTP exception occurred e=" + e);
            LOGGER.error("HTTP Error response = " + e.getResponseBodyAsString());
            e.printStackTrace();
            throw new Exception(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            LOGGER.error("Exception occurred while starting container e=" + e);
            LOGGER.error("Message= " + e.getMessage());
            LOGGER.error("Message= " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new Exception("failed to start container e=" + e.getMessage());
        }
        return result_map;
    }
}

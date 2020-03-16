package org.renci.mobius.api;

import org.json.simple.JSONObject;
import org.renci.mobius.controllers.MobiusController;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.MobiusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-16T15:34:23.050-04:00[America/New_York]")
@Controller
public class ListWorkflowsApiController implements ListWorkflowsApi {

    private static final Logger log = LoggerFactory.getLogger(ListWorkflowsApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public ListWorkflowsApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<MobiusResponse> listWorkflowsGet() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<MobiusResponse>(objectMapper.readValue("{\n  \"message\" : \"message\",\n  \"value\" : { },\n  \"version\" : \"version\",\n  \"status\" : 0\n}", MobiusResponse.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<MobiusResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        JSONObject output = new JSONObject();
        MobiusResponse resp = new MobiusResponse();
        resp.setVersion("0.1");
        HttpStatus status = HttpStatus.OK;
        try {
            resp.setValue(MobiusController.getInstance().listWorkflows());
            resp.setStatus(HttpStatus.OK.value());
            resp.setMessage("Success");
        }
        catch (MobiusException e) {
            log.error("Exception occurred e=" + e);
            e.printStackTrace();
            status = e.getStatus();
            resp.setStatus(status.value());
            resp.setMessage(e.getMessage());
        }
        catch (Exception e) {
            log.error("Exception occurred e=" + e);
            e.printStackTrace();
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            resp.setMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        }

        return new ResponseEntity<MobiusResponse>(resp, status);
    }

}

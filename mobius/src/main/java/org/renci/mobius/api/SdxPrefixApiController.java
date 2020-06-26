package org.renci.mobius.api;

import org.json.simple.JSONObject;
import org.renci.mobius.controllers.MobiusController;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.MobiusResponse;
import org.renci.mobius.model.SdxPrefix;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
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
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T09:55:10.838-04:00[America/New_York]")
@Controller
public class SdxPrefixApiController implements SdxPrefixApi {

    private static final Logger log = LoggerFactory.getLogger(SdxPrefixApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public SdxPrefixApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<MobiusResponse> sdxPrefixPost(@ApiParam(value = "" ,required=true )  @Valid @RequestBody SdxPrefix body
,@NotNull @ApiParam(value = "", required = true) @Valid @RequestParam(value = "workflowID", required = true) String workflowID
) {
        log.debug("IN");
        String accept = request.getHeader("Accept");
        MobiusResponse resp = new MobiusResponse();
        resp.setVersion("0.1");
        HttpStatus status = HttpStatus.OK;

        if (accept != null && accept.contains("application/json")) {
            JSONObject output = new JSONObject();

            try {
                log.debug("Invoking controller processNetwork");
                MobiusController.getInstance().processSdxPrefix(workflowID, body);
                log.debug("OK response");
                resp.setStatus(HttpStatus.OK.value());
                resp.setMessage("Success");
            } catch (MobiusException e) {
                log.error("Exception occurred e=" + e);
                e.printStackTrace();
                status = e.getStatus();
                resp.setStatus(status.value());
                resp.setMessage(e.getMessage());
            } catch (Exception e) {
                log.error("Exception occurred e=" + e);
                e.printStackTrace();
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                resp.setMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            }
            log.debug("OUT");

            return new ResponseEntity<MobiusResponse>(resp, status);
        }
        status = HttpStatus.BAD_REQUEST;
        resp.setStatus(HttpStatus.BAD_REQUEST.value());
        resp.setMessage(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return new ResponseEntity<MobiusResponse>(resp, status);
    }

}

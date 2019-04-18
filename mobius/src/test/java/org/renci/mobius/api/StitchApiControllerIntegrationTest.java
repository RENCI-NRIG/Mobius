package org.renci.mobius.api;

import org.renci.mobius.model.MobiusResponse;
import org.renci.mobius.model.StitchRequest;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StitchApiControllerIntegrationTest {

    @Autowired
    private StitchApi api;

    @Test
    public void stitchPostTest() throws Exception {
        StitchRequest body = new StitchRequest();
        String workflowID = "workflowID_example_stitch";
        ResponseEntity<MobiusResponse> responseEntity = api.stitchPost(body, workflowID);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

}

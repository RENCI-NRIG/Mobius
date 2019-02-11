package org.renci.mobius.api;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.MobiusResponse;

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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ComputeApiControllerIntegrationTest {

    @Autowired
    private ComputeApi api;

    @Test
    public void computePostTest() throws Exception {
        ComputeRequest body = new ComputeRequest();
        String workflowID = "workflowID_example_compute";
        ResponseEntity<MobiusResponse> responseEntity = api.computePost(body, workflowID);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

}

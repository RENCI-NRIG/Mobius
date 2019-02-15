package org.renci.mobius.api;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.renci.mobius.controllers.MobiusController;
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
public class WorkflowApiControllerIntegrationTest {

    @Autowired
    private WorkflowApi api;

    @Test
    public void workflowDeleteTest() throws Exception {
        String workflowID = "workflowID_example";
        ResponseEntity<MobiusResponse> responseEntity = api.workflowDelete(workflowID);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void workflowGetTest() throws Exception {
        String workflowID = "workflowID_example";
        ResponseEntity<MobiusResponse> responseEntity = api.workflowGet(workflowID);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void workflowPostTest() throws Exception {
        String workflowID = "workflowID_example";
        ResponseEntity<MobiusResponse> responseEntity = api.workflowPost(workflowID);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

}

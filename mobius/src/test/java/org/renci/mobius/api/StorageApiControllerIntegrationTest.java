package org.renci.mobius.api;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.renci.mobius.model.MobiusResponse;
import org.renci.mobius.model.StorageRequest;

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
public class StorageApiControllerIntegrationTest {

    @Autowired
    private StorageApi api;

    @Test
    public void storagePostTest() throws Exception {
        StorageRequest body = new StorageRequest();
        String workflowID = "workflowID_example";
        ResponseEntity<MobiusResponse> responseEntity = api.storagePost(body, workflowID);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

}

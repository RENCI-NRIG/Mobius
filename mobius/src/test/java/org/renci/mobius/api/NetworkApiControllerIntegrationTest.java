package org.renci.mobius.api;

import org.renci.mobius.model.MobiusResponse;
import org.renci.mobius.model.NetworkRequest;

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
public class NetworkApiControllerIntegrationTest {

    @Autowired
    private NetworkApi api;

    @Test
    public void networkPostTest() throws Exception {
        NetworkRequest body = new NetworkRequest();
        String workflowID = "workflowID_example";
        ResponseEntity<MobiusResponse> responseEntity = api.networkPost(body, workflowID);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

}

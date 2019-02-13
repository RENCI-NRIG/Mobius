package org.renci.mobius.controllers.chameleon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.log4j.Logger;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.SliceNotFoundOrDeadException;
import org.renci.mobius.model.ComputeRequest;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;

public class StackContext {
    private static final Logger LOGGER = Logger.getLogger( StackContext.class.getName() );

    private ComputeRequest lastRequest;
    private String sliceName;
    private boolean sendNotification;
    //private State state;
    private Date expiry;
    private ObjectMapper mapper;

    public StackContext(String sliceName) {
        this.sliceName = sliceName;
        this.lastRequest = null;
        sendNotification = false;
        //state = NULL;
        expiry = null;
        mapper = new ObjectMapper(new YAMLFactory());
    }

    public void setSendNotification(boolean value) {
        sendNotification = value;
    }
    public Date getExpiry() { return expiry; }
    public String getSliceName() {
        return sliceName;
    }
    public ComputeRequest getLastRequest() {
        return lastRequest;
    }

    public void setExpiry(String expiry) {
        long timestamp = Long.parseLong(expiry);
        this.expiry = new Date(timestamp);
    }

    public boolean canTriggerNotification() {
        /*if(sendNotification &&
                (state == STABLE_OK || state == STABLE_ERROR)) {
            return true;
        }*/
        return false;
    }
    public void stop() {
        LOGGER.debug("stop: IN");

        try {
            LOGGER.debug("Successfully deleted slice " + sliceName);
        }
        catch (Exception e){
            LOGGER.debug("Exception occured while deleting slice " + sliceName);
        }
        LOGGER.debug("stop: OUT");
    }

    private String generateYaml() {
        String yaml = null;//mapper.writeValueAsString();
        return yaml;
    }

    public int processCompute(List<String> flavorList, int nameIndex, ComputeRequest request) throws Exception {
        LOGGER.debug("processCompute: IN");

        try {
            throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
        }
        catch (MobiusException e) {
            LOGGER.error("Exception occurred =" + e);
            e.printStackTrace();
            throw e;
        }
        finally {
            LOGGER.debug("processCompute: OUT");
        }
    }
}

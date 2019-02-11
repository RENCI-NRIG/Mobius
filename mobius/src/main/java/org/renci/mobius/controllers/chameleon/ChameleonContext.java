package org.renci.mobius.controllers.chameleon;

import com.google.common.collect.Multimap;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.SliceNotFoundOrDeadException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;

public class ChameleonContext extends CloudContext {
    private static final Logger LOGGER = Logger.getLogger( ChameleonContext.class.getName() );

    private HashMap<String, StackContext> stackContextHashMap;

    public ChameleonContext(CloudContext.CloudType t, String s) {
        super(t, s);
    }
    protected void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("validateComputeRequest: IN");

        if(request.getCpus() > 0) {
            throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not supported yet");
        }

        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest);
        LOGGER.debug("validateComputeRequest: OUT");
    }

    @Override
    public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        validateComputeRequest(request, isFutureRequest);

        List<String> flavorList = ChameleonFlavorAlgo.determineFlavors(request.getCpus(), request.getRamPerCpus(), request.getDiskPerCpus(), request.isCoallocate());
        if(flavorList == null) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
        }

        String sliceName = null;

        switch (request.getSlicePolicy()) {
            case NEW:
                // Create new slice
                break;
            case DEFAULT:
                sliceName = findSlice(request);
                break;
            case EXISTING:
                sliceName = request.getSliceName();
                break;
            default:
                throw new MobiusException(HttpStatus.BAD_REQUEST, "Unspported SlicePolicy");
        }

        StackContext context = null;
        boolean addSliceToMaps = false;

        if(sliceName != null) {
            LOGGER.debug("Using existing context=" + sliceName);
            context = stackContextHashMap.get(sliceName);
        }
        else {
            context = new StackContext(sliceName);
            addSliceToMaps = true;
            LOGGER.debug("Created new context=" + sliceName);
        }

        try {
            nameIndex = context.processCompute(flavorList, nameIndex, request);

            sliceName = context.getSliceName();

            if(addSliceToMaps) {
                long timestamp = Long.parseLong(request.getLeaseEnd());
                Date expiry = new Date(timestamp * 1000);
                stackContextHashMap.put(sliceName, context);
                leaseEndTimeToSliceNameHashMap.put(expiry, sliceName);
                LOGGER.debug("Added " + sliceName + " with expiry= " + expiry);
            }
            return nameIndex;
        }
        catch (SliceNotFoundOrDeadException e) {
            handSliceNotFoundException(context.getSliceName());
            stackContextHashMap.remove(context.getSliceName());
            throw new MobiusException("Slice not found");
        }
        finally {
            LOGGER.debug("processCompute: OUT");
        }    }

    @Override
    public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }

    @Override
    public JSONObject doPeriodic() {
        return null;
    }

    @Override
    public JSONObject getStatus() {
        return null;
    }

    @Override
    public boolean containsSlice(String sliceName) {
        return stackContextHashMap.containsKey(sliceName);
    }

    @Override
    public void stop() {
        LOGGER.debug("stop: IN");
        StackContext context = null;
        for (HashMap.Entry<String, StackContext> entry:stackContextHashMap.entrySet()) {
            context = entry.getValue();
            context.stop();
        }
        stackContextHashMap.clear();
        LOGGER.debug("stop: OUT");
    }
}

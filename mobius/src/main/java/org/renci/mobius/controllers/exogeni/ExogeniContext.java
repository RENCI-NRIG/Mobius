package org.renci.mobius.controllers.exogeni;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.SliceNotFoundOrDeadException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;
import org.apache.log4j.Logger;


import java.util.*;

public class ExogeniContext extends CloudContext {
    private static final Logger LOGGER = Logger.getLogger( ExogeniContext.class.getName() );
    private HashMap<String, SliceContext> sliceContextHashMap;


    public ExogeniContext(CloudContext.CloudType t, String s) {
        super(t, s);
        sliceContextHashMap = new HashMap<>();
    }

    protected void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("validateComputeRequest: IN");

        if(request.getGpus() > 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Exogeni does not support Gpus");
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest);
        LOGGER.debug("validateComputeRequest: OUT");
    }

    @Override
    public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        LOGGER.debug("processCompute: IN");

        validateComputeRequest(request, isFutureRequest);

        List<String> flavorList = ExogeniFlavorAlgo.determineFlavors(request.getCpus(), request.getRamPerCpus(), request.getDiskPerCpus(), request.isCoallocate());
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

        SliceContext context = null;
        boolean addSliceToMaps = false;

        if(sliceName != null) {
            LOGGER.debug("Using existing context=" + sliceName);
            context = sliceContextHashMap.get(sliceName);
        }
        else {
            context = new SliceContext(sliceName);
            addSliceToMaps = true;
            LOGGER.debug("Created new context=" + sliceName);
        }

        try {
            nameIndex = context.processCompute(flavorList, nameIndex, request);

            sliceName = context.getSliceName();

            if(addSliceToMaps) {
                long timestamp = Long.parseLong(request.getLeaseEnd());
                Date expiry = new Date(timestamp * 1000);
                sliceContextHashMap.put(sliceName, context);
                leaseEndTimeToSliceNameHashMap.put(expiry, sliceName);
                LOGGER.debug("Added " + sliceName + " with expiry= " + expiry);
            }
            return nameIndex;
        }
        catch (SliceNotFoundOrDeadException e) {
            handSliceNotFoundException(context.getSliceName());
            sliceContextHashMap.remove(context);
            throw new MobiusException("Slice not found");
        }
        finally {
            LOGGER.debug("processCompute: OUT");
        }
    }

    @Override
    public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        LOGGER.debug("processStorageRequest: IN");
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest);

        String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
        if(sliceName == null) {
            throw new MobiusException("hostName not found in hostNameToSliceHashMap");
        }
        SliceContext context = sliceContextHashMap.get(sliceName);
        if(context == null) {
            throw new MobiusException("slice context not found");
        }
        try {
            nameIndex = context.processStorageRequest(request, nameIndex);
            return nameIndex;
        }
        catch (SliceNotFoundOrDeadException e) {
            handSliceNotFoundException(context.getSliceName());
            sliceContextHashMap.remove(context);
            throw new MobiusException("Slice not found");
        }
        finally {
            LOGGER.debug("processStorageRequest: OUT");
        }
    }

    @Override
    public JSONObject getStatus() throws Exception {
        LOGGER.debug("getStatus: IN");
        JSONArray array = new JSONArray();
        JSONObject retVal = null;

        SliceContext context = null;
        for (HashMap.Entry<String, SliceContext> entry:sliceContextHashMap.entrySet()) {
            context = entry.getValue();

            try {
                JSONObject object = context.status(hostNameSet);
                if(!object.isEmpty()) {
                    array.add(object);
                }
            }
            catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
            }
        }
        if(!array.isEmpty()) {
            retVal = new JSONObject();
            retVal.put(CloudContext.JsonKeySite, getSite());
            retVal.put(CloudContext.JsonKeySlices, array);
        }
        LOGGER.debug("getStatus: OUT");
        return retVal;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.debug("stop: IN");
        SliceContext context = null;
        for (HashMap.Entry<String, SliceContext> entry:sliceContextHashMap.entrySet()) {
            context = entry.getValue();
            context.stop();
        }
        sliceContextHashMap.clear();
        LOGGER.debug("stop: OUT");
    }

    @Override
    public JSONObject doPeriodic() {
        LOGGER.debug("doPeriodic: IN");
        SliceContext context = null;
        JSONObject retVal = null;
        JSONArray array = new JSONArray();
        hostNameToSliceNameHashMap.clear();
        leaseEndTimeToSliceNameHashMap.clear();
        hostNameSet.clear();
        Iterator<HashMap.Entry<String, SliceContext>> iterator = sliceContextHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry<String, SliceContext> entry = iterator.next();
            context = entry.getValue();
            Set<String> hostNames = new HashSet<>();
            JSONObject result = null;
            try {
                result = context.doPeriodic(hostNames);
            }
            catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                iterator.remove();
                continue;
            }
            hostNameSet.addAll(hostNames);
            for(String h : hostNames) {
                if(!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                    hostNameToSliceNameHashMap.put(h, context.getSliceName());
                }
            }
            if(result != null && !result.isEmpty()) {
                array.add(result);
            }
            leaseEndTimeToSliceNameHashMap.put(context.getExpiry(), context.getSliceName());
            triggerNotification |= context.canTriggerNotification();
            if(context.canTriggerNotification()) {
                context.setSendNotification(false);
            }
        }
        if(!array.isEmpty()) {
           retVal = new JSONObject();
           retVal.put(CloudContext.JsonKeySite, getSite());
           retVal.put(CloudContext.JsonKeySlices, array);
        }
        LOGGER.debug("doPeriodic: OUT");
        return retVal;
    }

    @Override
    public boolean containsSlice(String sliceName) {
        return sliceContextHashMap.containsKey(sliceName);
    }
}

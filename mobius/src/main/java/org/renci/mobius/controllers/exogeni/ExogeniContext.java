package org.renci.mobius.controllers.exogeni;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.FutureRequestException;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.controllers.SliceNotFoundOrDeadException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;
import org.apache.log4j.Logger;


import java.util.*;

public class ExogeniContext extends CloudContext {
    private static final Logger LOGGER = Logger.getLogger( ExogeniContext.class.getName() );
    private HashMap<String, SliceContext> sliceNameToSliceContextMap;
    private Multimap<Date, String> leaseEndTimeToSliceNameHashMap;
    private HashMap<String, String> hostNameToSliceNameHashMap;
    private String networkName;

    public ExogeniContext(CloudContext.CloudType t, String s) {
        super(t, s);
        sliceNameToSliceContextMap = new HashMap<>();
        leaseEndTimeToSliceNameHashMap = ArrayListMultimap.create();
        hostNameToSliceNameHashMap = new HashMap<>();
        networkName = "Network";
    }

    private void validateLeasTime(String startTime, String endTime, boolean isFutureRequest) throws Exception {
        LOGGER.debug("validateLeasTime: IN");
        long currTime = System.currentTimeMillis();
        long beginTimestamp = Long.parseLong(startTime) * 1000;
        long endTimestamp  = Long.parseLong(endTime) * 1000;

        if(beginTimestamp > currTime) {
            LOGGER.info("Future request to be started at " + beginTimestamp);
            throw new FutureRequestException("future request " + beginTimestamp);
        }

        // Ignore Start time check for requests triggered via periodic processing
        if(!isFutureRequest) {
            long diff = java.lang.Math.abs(currTime - beginTimestamp);
            if (diff > AllowedDeltaTimeInMsFromCurrentTime) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "startTime is before currentTime");
            }
        }

        if(endTimestamp < currTime){
            throw new MobiusException(HttpStatus.BAD_REQUEST, "endTime is before currTime");
        }
        if(endTimestamp - beginTimestamp <= minimumTimeDifInMs) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Diff between endTime and startTime is less than 24 hours");
        }
        LOGGER.debug("validateLeasTime: OUT");
    }

    private void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("validateComputeRequest: IN");

        if(request.getGpus() > 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Exogeni does not support Gpus");
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest);
        LOGGER.debug("validateComputeRequest: OUT");
    }

    private String findSlice(ComputeRequest request) {
        LOGGER.debug("findSlice: IN");
        if(leaseEndTimeToSliceNameHashMap.size() == 0) {
            return null;
        }

        long timestamp = Long.parseLong(request.getLeaseEnd());
        Date expiry = new Date(timestamp * 1000);

        String sliceName = null;
        if(leaseEndTimeToSliceNameHashMap.containsKey(expiry)) {
            sliceName = leaseEndTimeToSliceNameHashMap.get(expiry).iterator().next();
        }
        LOGGER.debug("findSlice: OUT");
        return sliceName;
    }

    @Override
    public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        LOGGER.debug("processCompute: IN");

        validateComputeRequest(request, isFutureRequest);

        List<String> flavorList = ExogeniFlavorAlgo.determineFlavors(request.getCpus(), request.getRamPerCpus(), request.getDiskPerCpus(), request.isCoallocate());
        if(flavorList == null) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
        }

        String sliceName = findSlice(request);

        SliceContext context = null;
        boolean addSliceToMaps = false;

        if(sliceName != null) {
            LOGGER.debug("Using existing context=" + sliceName);
            context = sliceNameToSliceContextMap.get(sliceName);
        }
        else {
            context = new SliceContext(sliceName);
            addSliceToMaps = true;
            LOGGER.debug("Created new context=" + sliceName);
        }

        int index = 0;
        try {
            index = context.processCompute(flavorList, nameIndex, request);
        }
        catch (SliceNotFoundOrDeadException e) {
            handSliceNotFoundException(context);
            sliceNameToSliceContextMap.remove(context);
            throw new MobiusException("Slice not found");
        }
        sliceName = context.getSliceName();

        if(addSliceToMaps) {
            long timestamp = Long.parseLong(request.getLeaseEnd());
            Date expiry = new Date(timestamp * 1000);
            sliceNameToSliceContextMap.put(sliceName, context);
            leaseEndTimeToSliceNameHashMap.put(expiry, sliceName);
            LOGGER.debug("Added " + sliceName + " with expiry= " + expiry);
        }
        LOGGER.debug("processCompute: OUT");
        return index;
    }

    @Override
    public JSONObject getStatus() throws Exception {
        LOGGER.debug("getStatus: IN");
        JSONArray array = new JSONArray();
        JSONObject retVal = null;

        SliceContext context = null;
        for (HashMap.Entry<String, SliceContext> entry:sliceNameToSliceContextMap.entrySet()) {
            context = entry.getValue();

            try {
                JSONObject object = context.status(hostNameSet);
                if(!object.isEmpty()) {
                    array.add(object);
                }
            }
            catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context);
                sliceNameToSliceContextMap.remove(context);
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
        for (HashMap.Entry<String, SliceContext> entry:sliceNameToSliceContextMap.entrySet()) {
            context = entry.getValue();
            context.stop();
        }
        sliceNameToSliceContextMap.clear();
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
        Iterator<HashMap.Entry<String, SliceContext>> iterator = sliceNameToSliceContextMap.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry<String, SliceContext> entry = iterator.next();
            context = entry.getValue();
            Set<String> hostNames = new HashSet<>();
            JSONObject result = null;
            try {
                result = context.doPeriodic(hostNames);
            }
            catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context);
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
    public void processStorageRequest(StorageRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("processStorageRequest: IN");
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest);

        String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
        if(sliceName == null) {
            throw new MobiusException("hostName not found in hostNameToSliceHashMap");
        }
        SliceContext context = sliceNameToSliceContextMap.get(sliceName);
        if(context == null) {
            throw new MobiusException("slice context not found");
        }
        try {
            context.processStorageRequest(request);
        }
        catch (SliceNotFoundOrDeadException e) {
            handSliceNotFoundException(context);
            sliceNameToSliceContextMap.remove(context);
            throw new MobiusException("Slice not found");
        }
        LOGGER.debug("processStorageRequest: OUT");
    }

    private void handSliceNotFoundException(SliceContext context) {
        LOGGER.debug("handSliceNotFoundException: IN");
        if(hostNameToSliceNameHashMap.containsValue(context.getSliceName())) {
            Iterator<HashMap.Entry<String, String>> iterator = hostNameToSliceNameHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(context.getSliceName())) {
                    iterator.remove();
                }
            }
        }
        if(leaseEndTimeToSliceNameHashMap.containsValue(context.getSliceName())) {
            Iterator<Map.Entry<Date, String>> iterator = leaseEndTimeToSliceNameHashMap.entries().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Date, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(context.getSliceName())) {
                    iterator.remove();
                }
            }
        }
        LOGGER.debug("handSliceNotFoundException: OUT");
    }
}

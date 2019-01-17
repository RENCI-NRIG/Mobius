package org.renci.mobius.controllers.exogeni;

import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.FutureRequestException;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;

import java.util.*;

public class ExogeniContext extends CloudContext {
    private HashMap<String, SliceContext> sliceNameToSliceContextMap;
    private HashMap<Date, String> leaseEndTimeToSliceNameHashMap;
    private HashMap<String, String> hostNameToSliceNameHashMap;
    private String networkName;

    public ExogeniContext(CloudContext.CloudType t, String s) {
        super(t, s);
        sliceNameToSliceContextMap = new HashMap<>();
        leaseEndTimeToSliceNameHashMap = new HashMap<>();
        hostNameToSliceNameHashMap = new HashMap<>();
        networkName = "Network";
    }

    private void validateLeasTime(String startTime, String endTime) throws Exception {
        long currTime = System.currentTimeMillis();
        long beginTimestamp = Long.parseLong(startTime) * 1000;
        long endTimestamp  = Long.parseLong(endTime) * 1000;

        /*if((beginTimestamp + AllowedDeltaTimeInMsFromCurrentTime) > currTime) {
            throw new FutureRequestException("Future request");
        }

        if((beginTimestamp + AllowedDeltaTimeInMsFromCurrentTime) < currTime) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "startTime is before currentTime");
        }*/

        if(endTimestamp < currTime){
            throw new MobiusException(HttpStatus.BAD_REQUEST, "endTime is before currTime");
        }
        if(endTimestamp - beginTimestamp <= minimumTimeDifInMs) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Diff between endTime and startTime is less than 24 hours");
        }
    }

    private void validateComputeRequest(ComputeRequest request) throws Exception {
        if(request.getGpus() > 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Exogeni does not support Gpus");
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd());
    }

    // TODO make it more effificent; also handle listResources
    private List<String> determineFlavors(ComputeRequest request) throws Exception{
        List<String> result = new LinkedList<String>();
        // TODO ram and disk unit
        Integer requestedCpus = request.getCpus();
        int i =0;
        if(requestedCpus != 0 && request.getRamPerCpus() <= Flavor.Small.getRamPerCpu()) {
            if(request.getDiskPerCpus() <= Flavor.Small.getDiskPerCpu()) {
                for(i =0 ; i< requestedCpus/Flavor.Small.getCpus(); i=i+Flavor.Small.getCpus()) {
                    result.add(Flavor.Small.getName());
                }
                requestedCpus -= i;
            }
        }
        if(requestedCpus != 0 && request.getRamPerCpus() <= Flavor.Medium.getRamPerCpu()) {
            if(request.getDiskPerCpus() <= Flavor.Medium.getDiskPerCpu()) {
                for(i =0 ; i< requestedCpus/Flavor.Medium.getCpus(); i=i+Flavor.Medium.getCpus()) {
                    result.add(Flavor.Medium.getName());
                }
                requestedCpus -= i;
            }
        }
        if(requestedCpus != 0 && request.getRamPerCpus() <= Flavor.Large.getRamPerCpu()) {
            if(request.getDiskPerCpus() <= Flavor.Large.getDiskPerCpu()) {
                for(i =0 ; i< requestedCpus/Flavor.Large.getCpus(); i=i+Flavor.Large.getCpus()) {
                    result.add(Flavor.Large.getName());
                }
                requestedCpus -= i;
            }
        }
        if(requestedCpus != 0 && request.getRamPerCpus() <= Flavor.ExtraLarge.getRamPerCpu() ) {
            if(request.getDiskPerCpus() <= Flavor.ExtraLarge.getDiskPerCpu()) {
                for(i =0 ; i< requestedCpus/Flavor.ExtraLarge.getCpus(); i=i+Flavor.ExtraLarge.getCpus()) {
                    result.add(Flavor.ExtraLarge.getName());
                }
                requestedCpus -= i;
            }
        }
        if(requestedCpus != 0 && request.getRamPerCpus() <= Flavor.BareMetal.getRamPerCpu() ) {
            if(request.getDiskPerCpus() <= Flavor.BareMetal.getDiskPerCpu()) {
                for(i =0 ; i< requestedCpus/Flavor.BareMetal.getCpus(); i=i+Flavor.BareMetal.getCpus()) {
                    result.add(Flavor.BareMetal.getName());
                }
                requestedCpus -= i;
            }
        }
        if(requestedCpus != 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Unable to determine nodes");
        }
        return result;
    }

    private String findSlice(ComputeRequest request) {
        if(leaseEndTimeToSliceNameHashMap.size() == 0) {
            return null;
        }

        long timestamp = Long.parseLong(request.getLeaseEnd());
        Date expiry = new Date(timestamp * 1000);

        String sliceName = null;
        if(leaseEndTimeToSliceNameHashMap.containsKey(expiry)) {
            sliceName = leaseEndTimeToSliceNameHashMap.get(expiry);
        }

        return sliceName;
    }

    @Override
    public int processCompute(String workflowId, ComputeRequest request, int nameIndex) throws Exception {

        validateComputeRequest(request);

        List<String> flavorList = determineFlavors(request);

        String sliceName = findSlice(request);

        SliceContext context = null;
        boolean addSliceToMaps = false;

        if(sliceName != null) {
            System.out.println("Using existing context=" + sliceName);
            context = sliceNameToSliceContextMap.get(sliceName);
        }
        else {
            context = new SliceContext(sliceName);
            addSliceToMaps = true;
            System.out.println("Created new context=" + sliceName);
        }

        int index = context.processCompute(flavorList, nameIndex, request);
        sliceName = context.getSliceName();

        if(addSliceToMaps) {
            long timestamp = Long.parseLong(request.getLeaseEnd());
            Date expiry = new Date(timestamp * 1000);
            sliceNameToSliceContextMap.put(sliceName, context);
            leaseEndTimeToSliceNameHashMap.put(expiry, sliceName);
            System.out.println("Added " + sliceName + " with expiry= " + expiry);
        }
        return index;
    }

    @Override
    public String getStatus() throws Exception {
        JSONArray array = new JSONArray();

        SliceContext context = null;
        for (HashMap.Entry<String, SliceContext> entry:sliceNameToSliceContextMap.entrySet()) {
            context = entry.getValue();
            JSONObject object = context.status(hostNameSet);
            if(!object.isEmpty()) {
                array.add(object);
            }
        }
        return array.toString();
    }

    @Override
    public void stop() throws Exception {
        SliceContext context = null;
        for (HashMap.Entry<String, SliceContext> entry:sliceNameToSliceContextMap.entrySet()) {
            context = entry.getValue();
            context.stop();
        }
        sliceNameToSliceContextMap.clear();
    }

    @Override
    public String doPeriodic() {
        String retVal = null;
        SliceContext context = null;
        JSONArray array = new JSONArray();
        boolean sendNotification = false;
        for (HashMap.Entry<String, SliceContext> entry:sliceNameToSliceContextMap.entrySet()) {
            context = entry.getValue();
            Set<String> hostNames = new HashSet<>();
            Pair<Boolean, JSONObject> result = context.doPeriodic(hostNames);
            hostNameSet.addAll(hostNames);
            for(String h : hostNames) {
                if(!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                    hostNameToSliceNameHashMap.put(h, context.getSliceName());
                }
            }
            if(result.getValue() != null && !result.getValue().isEmpty()) {
                array.add(result.getValue());
            }
            sendNotification |= result.getKey();
        }
        if(sendNotification && !array.isEmpty()) {
            retVal = array.toString();
        }
        return retVal;
    }

    @Override
    public void processStorageRequest(StorageRequest request) throws Exception {
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd());

        String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
        if(sliceName == null) {
            throw new MobiusException("hostName not found in hostNameToSliceHashMap");
        }
        SliceContext context = sliceNameToSliceContextMap.get(sliceName);
        if(context == null) {
            throw new MobiusException("slice context not found");
        }
        context.processStorageRequest(request);
    }
}

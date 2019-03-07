package org.renci.mobius.controllers.chameleon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.CloudContext;
import org.renci.mobius.controllers.MobiusException;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.StorageRequest;

import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;

public class ChameleonContext extends CloudContext {
    private static final Logger LOGGER = Logger.getLogger( ChameleonContext.class.getName() );
    private static final Long maxDiffInSeconds = 604800L;

    private HashMap<String, StackContext> stackContextHashMap;


    public ChameleonContext(CloudContext.CloudType t, String s) {
        super(t, s);
        stackContextHashMap = new HashMap<>();
    }
    protected void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("validateComputeRequest: IN");
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, maxDiffInSeconds);
        LOGGER.debug("validateComputeRequest: OUT");
    }

    @Override
    public JSONArray toJson() {
        synchronized (this) {
            JSONArray slices = null;
            if (stackContextHashMap != null && stackContextHashMap.size() != 0) {
                slices = new JSONArray();
                StackContext context = null;
                for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                    context = entry.getValue();
                    JSONObject slice = context.toJson();
                    slices.add(slice);
                }
            }
            return slices;
        }
    }

    @Override
    public void fromJson(JSONArray array) {
        synchronized (this) {
            if (array != null) {
                for (Object object : array) {
                    JSONObject slice = (JSONObject) object;
                    String sliceName = (String) slice.get("name");
                    LOGGER.debug("fromJson(): sliceName=" + sliceName);
                    StackContext sliceContext = new StackContext(sliceName);
                    sliceContext.fromJson(slice);
                    stackContextHashMap.put(sliceName, sliceContext);
                }
            } else {
                LOGGER.error("fromJson(): Null array passed");
            }
        }
    }

    @Override
    public int processCompute(String workflowId, ComputeRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            validateComputeRequest(request, isFutureRequest);

            Map<String, Integer> flavorList = ChameleonFlavorAlgo.determineFlavors(request.getCpus(),
                    request.getRamPerCpus(), request.getDiskPerCpus(), request.isCoallocate());

            if (flavorList == null) {
                throw new MobiusException(HttpStatus.BAD_REQUEST, "None of the flavors can satisfy compute request");
            }

            String sliceName = null;
            StackContext context = new StackContext(sliceName);

            try {

                // For chameleon; slice per request mechanism is followed
                nameIndex = context.processCompute(flavorList, nameIndex, request);
                LOGGER.debug("Created new context=" + sliceName);

                sliceName = context.getSliceName();
                stackContextHashMap.put(sliceName, context);
                LOGGER.debug("Added " + sliceName);

                return nameIndex;
            } finally {
                LOGGER.debug("processCompute: OUT");
            }
        }
    }

    @Override
    public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            throw new MobiusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
        }
    }

    @Override
    public JSONObject doPeriodic() {
        synchronized (this) {
            LOGGER.debug("doPeriodic: IN");
            StackContext context = null;
            JSONObject retVal = null;
            JSONArray array = new JSONArray();
            hostNameToSliceNameHashMap.clear();
            hostNameSet.clear();
            Iterator<HashMap.Entry<String, StackContext>> iterator = stackContextHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, StackContext> entry = iterator.next();
                context = entry.getValue();
                Set<String> hostNames = new HashSet<>();
                JSONObject result = null;
                result = context.doPeriodic(hostNames);

                hostNameSet.addAll(hostNames);
                for (String h : hostNames) {
                    if (!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                        hostNameToSliceNameHashMap.put(h, context.getSliceName());
                    }
                }
                if (result != null && !result.isEmpty()) {
                    array.add(result);
                }
                triggerNotification |= context.canTriggerNotification();
                if (context.canTriggerNotification()) {
                    context.setNotificationSent();
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }

            LOGGER.debug("doPeriodic: OUT");
            return retVal;
        }
    }

    @Override
    public JSONObject getStatus() {
        synchronized (this) {
            LOGGER.debug("getStatus: IN");
            JSONObject retVal = null;
            JSONArray array = new JSONArray();

            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                JSONObject object = context.status(hostNameSet);
                if (!object.isEmpty()) {
                    array.add(object);
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }
            LOGGER.debug("getStatus: OUT");
            return retVal;        }
    }

    @Override
    public boolean containsSlice(String sliceName) {
        return stackContextHashMap.containsKey(sliceName);
    }

    @Override
    public void stop() {
        synchronized (this) {
            LOGGER.debug("stop: IN");
            StackContext context = null;
            for (HashMap.Entry<String, StackContext> entry : stackContextHashMap.entrySet()) {
                context = entry.getValue();
                context.stop();
            }
            stackContextHashMap.clear();
            LOGGER.debug("stop: OUT");
        }
    }
}

package org.renci.mobius.controllers.exogeni;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.InetAddresses;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.renci.mobius.controllers.*;
import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;
import org.springframework.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.*;

/*
 * @brief class represents context for all resources on a specific region on exogeni. It maintains
 *        SliceContext per slice.
 *
 * @author kthare10
 */
public class ExogeniContext extends CloudContext {
    private static final Logger LOGGER = LogManager.getLogger( ExogeniContext.class.getName() );
    private HashMap<String, SliceContext> sliceContextHashMap;
    private Multimap<Date, String> leaseEndTimeToSliceNameHashMap;


    /*
     * @brief constructor
     *
     * @param t - cloud type
     * @param s - site
     * @param workflowId - workflow id
     *
     *
     */
    public ExogeniContext(CloudContext.CloudType t, String s, String workflowId) {
        super(t, s, workflowId);
        sliceContextHashMap = new HashMap<>();
        leaseEndTimeToSliceNameHashMap = ArrayListMultimap.create();
    }

    /*
     * @brief function to generate JSONArray representing all the slice contexts held in this context
     *
     * @return JSONArray
     */
    @Override
    public JSONArray toJson() {
        synchronized (this) {
            JSONArray slices = null;
            if (sliceContextHashMap != null && sliceContextHashMap.size() != 0) {
                slices = new JSONArray();
                SliceContext context = null;
                for (HashMap.Entry<String, SliceContext> entry : sliceContextHashMap.entrySet()) {
                    context = entry.getValue();
                    JSONObject slice = new JSONObject();
                    slice.put("name", context.getSliceName());
                    if (context.getExpiry() != null) {
                        slice.put("expiry", Long.toString(context.getExpiry().getTime()));
                    }
                    slices.add(slice);
                }
            }
            return slices;
        }
    }

    /*
     * @brief build the context from JSONArray read from database; invoked when contexts are loaded
     *        on mobius restart
     *
     * @param array - json array representing all the slice contexts
     */
    @Override
    public void fromJson(JSONArray array) {
        synchronized (this) {
            if (array != null) {
                for (Object object : array) {
                    JSONObject slice = (JSONObject) object;
                    String sliceName = (String) slice.get("name");
                    LOGGER.debug("sliceName=" + sliceName);
                    SliceContext sliceContext = new SliceContext(sliceName);
                    String expiry = (String) slice.get("expiry");
                    LOGGER.debug("expiry=" + expiry);
                    if (expiry != null) {
                        sliceContext.setExpiry(expiry);
                    }
                    sliceContextHashMap.put(sliceName, sliceContext);
                }
            } else {
                LOGGER.error("Null array passed");
            }
        }
    }

    /*
     * @brief add cloud specific info to JSON Object representing ExogeniContext;
     *        JSON Object is saved to database
     *
     * @param object - json object representing ExogeniContext
     */
    @Override
    public JSONObject addCloudSpecificDataToJson(JSONObject object) {
        return object;
    }

    /*
     * @brief function to load cloud specific data from JSON Object representing ExogeniContext
     *
     * @param object - json object representing ExogeniContext
     */
    @Override
    public void loadCloudSpecificDataFromJson(JSONObject object) {
    }

    /*
     * @brief validate compute request; ignore leaseStart and leaseEnd time validation for future requests
     *
     * @param request - compute request
     * @param isFutureRequest - flag indicating if request is future request
     *
     * @throws Exception in case validation fails
     *
     */
    protected void validateComputeRequest(ComputeRequest request, boolean isFutureRequest) throws Exception {
        LOGGER.debug("IN");

        if(request.getGpus() > 0) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Exogeni does not support Gpus");
        }

        if(!request.isCoallocate() && request.getIpAddress() != null) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "IP address can only be specified with coallocate=true");
        }

        if(request.getIpAddress() != null &&  !InetAddresses.isInetAddress(request.getIpAddress())) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Not a valid IP address");
        }

        if(request.getHostNamePrefix() != null && !request.getHostNamePrefix().matches("[a-zA-Z]+")) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "Host Name prefix can only contain alphabet characters");
        }

        if(request.getLeaseEnd() == null) {
            throw new MobiusException(HttpStatus.BAD_REQUEST, "No end time specified");
        }
        if(request.getLeaseStart() == null) {
            Date now = new Date();
            long milliseconds = now.getTime()/1000;

            request.setLeaseStart(Long.toString(milliseconds));
        }
        validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, null);
        LOGGER.debug("OUT");
    }

    /*
     * @brief function to process compute request
     *
     * @param request - compute request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return ComputeResponse
     */
    @Override
    public ComputeResponse processCompute(ComputeRequest request, int nameIndex, int spNameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN");

            validateComputeRequest(request, isFutureRequest);

            List<String> flavorList = ExogeniFlavorAlgo.determineFlavors(request.getCpus(), request.getRamPerCpus(), request.getDiskPerCpus(), request.isCoallocate());
            if (flavorList == null) {
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

            if (sliceName != null) {
                LOGGER.debug("Using existing context=" + sliceName);
                context = sliceContextHashMap.get(sliceName);
            } else {
                context = new SliceContext(sliceName);
                addSliceToMaps = true;
                LOGGER.debug("Created new context=" + sliceName);
            }

            try {
                ComputeResponse response = context.processCompute(flavorList, nameIndex, spNameIndex, request);

                sliceName = context.getSliceName();

                if (addSliceToMaps) {

                    Date expiry = context.getExpiry();
                    sliceContextHashMap.put(sliceName, context);
                    if(expiry != null) {
                        leaseEndTimeToSliceNameHashMap.put(expiry, sliceName);
                        LOGGER.debug("Added " + sliceName + " with expiry= " + expiry + " ");
                    }
                }
                return response;
            } catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
                throw new MobiusException("Slice not found");
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }

    /*
     * @brief function to process storge request
     *
     * @param request - storge request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     */
    @Override
    public int processStorageRequest(StorageRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN");
            validateLeasTime(request.getLeaseStart(), request.getLeaseEnd(), isFutureRequest, null);

            String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap");
            }
            SliceContext context = sliceContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }
            try {
                nameIndex = context.processStorageRequest(request, nameIndex);
                return nameIndex;
            } catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
                throw new MobiusException("Slice not found");
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }

    /*
     * @brief function to process a stitch request;
     *
     * @param request - stitch request
     * @param nameIndex - number representing index to be added to instance name
     * @param isFutureRequest - true in case this is a future request; false otherwise
     *
     * @throws Exception in case of error
     *
     * @return number representing index to be added for the instance name
     *
     */
    @Override
    public int processStitchRequest(StitchRequest request, int nameIndex, boolean isFutureRequest) throws Exception {
        synchronized (this) {
            LOGGER.debug("IN");

            String sliceName = hostNameToSliceNameHashMap.get(request.getTarget());
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap");
            }
            SliceContext context = sliceContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }
            try {
                nameIndex = context.processStitchRequest(request, nameIndex);
                return nameIndex;
            } catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
                throw new MobiusException("Slice not found");
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }

    /*
     * @brief function to check get status for the context
     *
     * @return JSONObject representing status
     */
    @Override
    public JSONObject getStatus() throws Exception {
        synchronized (this) {
            LOGGER.debug("IN");
            JSONObject retVal = null;
            JSONArray array = new JSONArray();

            hostNameToSliceNameHashMap.clear();
            leaseEndTimeToSliceNameHashMap.clear();
            hostNameSet.clear();

            SliceContext context = null;
            for (HashMap.Entry<String, SliceContext> entry : sliceContextHashMap.entrySet()) {
                context = entry.getValue();

                try {
                    JSONObject object = context.status(hostNameSet);
                    if (!object.isEmpty()) {
                        array.add(object);
                    }
                } catch (SliceNotFoundOrDeadException e) {
                    handSliceNotFoundException(context.getSliceName());
                    sliceContextHashMap.remove(context);
                    continue;
                }
                for (String h : hostNameSet) {
                    if (!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                        LOGGER.debug("Adding " + h + "=>" + context.getSliceName() + " to hostNameToSliceNameHashMap");
                        hostNameToSliceNameHashMap.put(h, context.getSliceName());
                    }
                }

                // TODO find a way to reload expiryTime
                if (context.getExpiry() != null) {
                    leaseEndTimeToSliceNameHashMap.put(context.getExpiry(), context.getSliceName());
                }
                triggerNotification |= context.canTriggerNotification();
                if (context.canTriggerNotification()) {
                    context.setSendNotification(false);
                }

            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }
            LOGGER.debug("OUT");
            return retVal;
        }
    }

    /*
     * @brief function to release all resources associated with this context
     */
    @Override
    public void stop() throws Exception {
        synchronized (this) {
            LOGGER.debug("IN");
            SliceContext context = null;
            for (HashMap.Entry<String, SliceContext> entry : sliceContextHashMap.entrySet()) {
                context = entry.getValue();
                context.stop();
            }
            sliceContextHashMap.clear();
            LOGGER.debug("OUT");
        }
    }

    /*
     * @brief performs following periodic actions
     *        - Reload hostnames of all instances
     *        - Reload hostNameToSliceNameHashMap
     *        - Determine if notification to pegasus should be triggered
     *        - Build notification JSON object
     *
     * @return JSONObject representing notification for context to be sent to pegasus
     */
    @Override
    public JSONObject doPeriodic() {
        synchronized (this) {
            LOGGER.debug("IN");
            SliceContext context = null;
            JSONObject retVal = null;
            JSONArray array = new JSONArray();
            LOGGER.debug("Clearing hostNameToSliceNameHashMap");
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
                } catch (SliceNotFoundOrDeadException e) {
                    handSliceNotFoundException(context.getSliceName());
                    iterator.remove();
                    continue;
                }
                hostNameSet.addAll(hostNames);
                for (String h : hostNames) {
                    if (!hostNameToSliceNameHashMap.containsKey(h) && context.getSliceName() != null) {
                        LOGGER.debug("Adding " + h + "=>" + context.getSliceName() + " to hostNameToSliceNameHashMap");
                        hostNameToSliceNameHashMap.put(h, context.getSliceName());
                    }
                }
                if (result != null && !result.isEmpty()) {
                    array.add(result);
                }
                // TODO find a way to reload expiryTime
                if (context.getExpiry() != null) {
                    leaseEndTimeToSliceNameHashMap.put(context.getExpiry(), context.getSliceName());
                }
                triggerNotification |= context.canTriggerNotification();
                if (context.canTriggerNotification()) {
                    context.setSendNotification(false);
                }
            }
            if (!array.isEmpty()) {
                retVal = new JSONObject();
                retVal.put(CloudContext.JsonKeySite, getSite());
                retVal.put(CloudContext.JsonKeySlices, array);
            }

            LOGGER.debug("OUT");
            return retVal;
        }
    }

    /*
     * @brief function to check if an instance with this hostname exists in this context
     *
     * @return true if hostname exists; false otherwise
     */
    @Override
    public boolean containsSlice(String sliceName) {
        return sliceContextHashMap.containsKey(sliceName);
    }

    /*
     * @brief find slice with lease time if exists
     *
     * @param request - compute request
     *
     * @return slice name
     */
    protected String findSlice(ComputeRequest request) {
        LOGGER.debug("IN");
        if(leaseEndTimeToSliceNameHashMap.size() == 0) {
            LOGGER.debug("OUT - leaseEndTimeToSliceNameHashMap empty");
            return null;
        }

        if(request.getLeaseEnd() == null) {
            LOGGER.debug("OUT - getLeaseEnd null");
            return null;
        }

        long timestamp = Long.parseLong(request.getLeaseEnd());
        Date expiry = new Date(timestamp * 1000);

        String sliceName = null;
        if(leaseEndTimeToSliceNameHashMap.containsKey(expiry)) {
            sliceName = leaseEndTimeToSliceNameHashMap.get(expiry).iterator().next();
        }
        LOGGER.debug("OUT");
        return sliceName;
    }

    /*
     * @brief function to handle slice not found
     *
     * @param sliceName - slice name
     */
    protected void handSliceNotFoundException(String sliceName) {
        LOGGER.debug("IN");
        if(hostNameToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<HashMap.Entry<String, String>> iterator = hostNameToSliceNameHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<String, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        if(leaseEndTimeToSliceNameHashMap.containsValue(sliceName)) {
            Iterator<Map.Entry<Date, String>> iterator = leaseEndTimeToSliceNameHashMap.entries().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Date, String> entry = iterator.next();
                if(entry.getValue().equalsIgnoreCase(sliceName)) {
                    iterator.remove();
                }
            }
        }
        LOGGER.debug("OUT");
    }

    /*
     * @brief function to stitch to sdx and advertise a prefix for add operation and unstitch in case of delete
     *
     * @param hostname - hostname
     * @param ip - ip
     * @param subnet - subnet
     * @param action - action
     * @param destHostName - destHostName
     * @param sdxStitchPortInterfaceIP - sdxStitchPortInterfaceIP (used only for chameleon)
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequestSetupStitchingAndRoute(String hostname, String ip, String subnet,
                                                            NetworkRequest.ActionEnum action, String destHostName,
                                                            String sdxStitchPortInterfaceIP) throws Exception{
        synchronized (this) {
            LOGGER.debug("IN hostname=" + hostname + " ip=" + ip + " subnet=" + subnet + " action=" + action
                    + " destHostName=" + destHostName + " hostNameToSliceNameHashMap=" + hostNameToSliceNameHashMap.toString());

            String sliceName = hostNameToSliceNameHashMap.get(hostname);
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap=" + hostNameToSliceNameHashMap.toString());
            }
            String destSliceName = hostNameToSliceNameHashMap.get(destHostName);
            if (destSliceName != null) {
                if (sliceName.equalsIgnoreCase(destSliceName)) {
                    throw new MobiusException("destination and source cannot be in the same slice");
                }
            }
            SliceContext context = sliceContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }
            try {
                context.processNetworkRequestSetupStitchingAndRoute(hostname, ip, subnet, action);
            } catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
                throw new MobiusException("Slice not found");
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }

    /*
     * @brief function to connect the link between source and destination subnet
     *
     * @param hostname - hostname
     * @param subnet1 - subnet1
     * @param subnet2 - subnet2
     * @param bandwidth - bandwidth
     * @param destinationIP - destinationIP
     * @param sdxStitchPortInterfaceIP - sdxStitchPortInterfaceIP (used only for chameleon)
     *
     * @throws Exception in case of error
     *
     */
    public void processNetworkRequestLink(String hostname, String subnet1, String subnet2, String bandwidth,
                                          String destinationIP, String sdxStitchPortInterfaceIP) throws Exception{
        synchronized (this) {
            LOGGER.debug("IN: hostname=" + hostname + " subnet1=" + subnet1 + " subnet2=" + subnet2 +
                    " hostNameToSliceNameHashMap=" + hostNameToSliceNameHashMap.toString());
            String sliceName = hostNameToSliceNameHashMap.get(hostname);
            if (sliceName == null) {
                throw new MobiusException("hostName not found in hostNameToSliceHashMap=" + hostNameToSliceNameHashMap.toString());
            }
            SliceContext context = sliceContextHashMap.get(sliceName);
            if (context == null) {
                throw new MobiusException("slice context not found");
            }
            try {
                context.processNetworkRequestLink(hostname, subnet1, subnet2, bandwidth, destinationIP);
            } catch (SliceNotFoundOrDeadException e) {
                handSliceNotFoundException(context.getSliceName());
                sliceContextHashMap.remove(context);
                throw new MobiusException("Slice not found");
            } finally {
                LOGGER.debug("OUT");
            }
        }
    }
}

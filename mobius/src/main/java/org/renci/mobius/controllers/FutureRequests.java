package org.renci.mobius.controllers;

import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StitchRequest;
import org.renci.mobius.model.StorageRequest;

/*
 * @brief class represents data structures which maintains all the future requests
 *
 * @author kthare10
 */
import java.util.LinkedList;
import java.util.List;

public class FutureRequests {
    private List<ComputeRequest> futureComputeRequests;
    private List<NetworkRequest> futureNetworkRequests;
    private List<StorageRequest> futureStorageRequests;
    private List<StitchRequest> futureStitchRequests;

    /*
     * @brief constructor
     */
    public FutureRequests() {
        futureComputeRequests = new LinkedList<>();
        futureNetworkRequests = new LinkedList<>();
        futureStorageRequests = new LinkedList<>();
        futureStitchRequests = new LinkedList<>();
    }

    /*
     * @brief return true if no future requests; false otherwise
     *
     * @return true if no future requests; false otherwise
     */
    public boolean isEmpty() {
        return futureComputeRequests.isEmpty() && futureNetworkRequests.isEmpty() &&
                futureStorageRequests.isEmpty() && futureStitchRequests.isEmpty();
    }

    /*
     * @brief return list of future compute requests
     *
     * @return list of future compute requests
     */
    public List<ComputeRequest> getFutureComputeRequests() {
        return futureComputeRequests;
    }

    /*
     * @brief return list of future network requests
     *
     * @return list of future network requests
     */
    public List<NetworkRequest> getFutureNetworkRequests() {
        return futureNetworkRequests;
    }

    /*
     * @brief return list of future storage requests
     *
     * @return list of future storage requests
     */
    public List<StorageRequest> getFutureStorageRequests() {
        return futureStorageRequests;
    }

    /*
     * @brief return list of future stitch requests
     *
     * @return list of future stitch requests
     */
    public List<StitchRequest> getFutureStitchRequests() {
        return futureStitchRequests;
    }

    /*
     * @brief add compute request
     *
     * @param req - compute request to be added
     */
    public void add(ComputeRequest req) {
        futureComputeRequests.add(req);
    }
    /*
     * @brief remove compute request
     *
     * @param request - compute request to be remove
     */
    public void remove(ComputeRequest request) {
        futureComputeRequests.remove(request);
    }

    /*
     * @brief add network request
     *
     * @param req - network request to be added
     */
    public void add(NetworkRequest req) {
        futureNetworkRequests.add(req);
    }

    /*
     * @brief remove network request
     *
     * @param request - network request to be remove
     */
    public void remove(NetworkRequest request) {
        futureNetworkRequests.remove(request);
    }

    /*
     * @brief add storage request
     *
     * @param req - storage request to be added
     */
    public void add(StorageRequest req) {
        futureStorageRequests.add(req);
    }

    /*
     * @brief remove storage request
     *
     * @param request - storage request to be remove
     */
    public void remove(StorageRequest request) {
        futureStorageRequests.remove(request);
    }

    /*
     * @brief add stitch request
     *
     * @param req - stitch request to be added
     */
    public void add(StitchRequest req) {
        futureStitchRequests.add(req);
    }

    /*
     * @brief remove stitch request
     *
     * @param request - stitch request to be remove
     */
    public void remove(StitchRequest request) {
        futureStitchRequests.remove(request);
    }

    /*
     * @brief clear all datastructures
     *
     */
    public void clear() {
        futureComputeRequests.clear();
        futureStorageRequests.clear();
        futureNetworkRequests.clear();
        futureStitchRequests.clear();
    }
}

package org.renci.mobius.controllers;

import org.renci.mobius.model.ComputeRequest;
import org.renci.mobius.model.NetworkRequest;
import org.renci.mobius.model.StorageRequest;

import java.util.LinkedList;
import java.util.List;

public class FutureRequests {
    private List<ComputeRequest> futureComputeRequests;
    private List<NetworkRequest> futureNetworkRequests;
    private List<StorageRequest> futureStorageRequests;

    public FutureRequests() {
        futureComputeRequests = new LinkedList<>();
        futureNetworkRequests = new LinkedList<>();
        futureStorageRequests = new LinkedList<>();
    }

    public boolean isEmpty() {
        return futureComputeRequests.isEmpty() && futureNetworkRequests.isEmpty() && futureStorageRequests.isEmpty();
    }

    public List<ComputeRequest> getFutureComputeRequests() {
        return futureComputeRequests;
    }

    public List<NetworkRequest> getFutureNetworkRequests() {
        return futureNetworkRequests;
    }

    public List<StorageRequest> getFutureStorageRequests() {
        return futureStorageRequests;
    }

    public void add(ComputeRequest req) {
        futureComputeRequests.add(req);
    }

    public void remove(ComputeRequest request) {
        futureComputeRequests.remove(request);
    }
    public void add(NetworkRequest req) {
        futureNetworkRequests.add(req);
    }

    public void remove(NetworkRequest request) {
        futureNetworkRequests.remove(request);
    }
    public void add(StorageRequest req) {
        futureStorageRequests.add(req);
    }

    public void remove(StorageRequest request) {
        futureStorageRequests.remove(request);
    }

    public void clear() {
        futureComputeRequests.clear();
        futureStorageRequests.clear();
        futureNetworkRequests.clear();
    }
}

package com.hubspot.baragon.managers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.BasePathConflictException;
import com.hubspot.baragon.exceptions.MissingLoadBalancerGroupException;
import com.hubspot.baragon.models.*;

import java.util.Collection;
import java.util.List;

@Singleton
public class RequestManager {
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;

  @Inject
  public RequestManager(BaragonRequestDatastore requestDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore,
                        BaragonStateDatastore stateDatastore) {
    this.requestDatastore = requestDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
  }

  public Optional<BaragonRequest> getRequest(String requestId) {
    return requestDatastore.getRequest(requestId);
  }

  public Optional<InternalRequestStates> getRequestState(String requestId) {
    return requestDatastore.getRequestState(requestId);
  }

  public void setRequestState(String requestId, InternalRequestStates state) {
    requestDatastore.setRequestState(requestId, state);
  }

  public void setRequestMessage(String requestId, String message) {
    requestDatastore.setRequestMessage(requestId, message);
  }

  public List<QueuedRequestId> getQueuedRequestIds() {
    return requestDatastore.getQueuedRequestIds();
  }

  public void removeQueuedRequest(QueuedRequestId queuedRequestId) {
    requestDatastore.removeQueuedRequest(queuedRequestId);
  }

  public Optional<BaragonResponse> getResponse(String requestId) {
    final Optional<InternalRequestStates> maybeStatus = requestDatastore.getRequestState(requestId);

    if (!maybeStatus.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new BaragonResponse(requestId, maybeStatus.get().toRequestState(), requestDatastore.getRequestMessage(requestId)));
  }

  private void ensureBasePathAvailable(BaragonRequest request) throws BasePathConflictException {
    final Service service = request.getLoadBalancerService();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, service.getServiceBasePath());
      if (maybeServiceId.isPresent() && !maybeServiceId.equals(service.getServiceId())) {
        throw new BasePathConflictException(request);
      }
    }
  }

  private void ensureRequestedLoadBalancersExist(BaragonRequest request) throws MissingLoadBalancerGroupException {
    final Service service = request.getLoadBalancerService();
    final Collection<String> loadBalancerGroups = loadBalancerDatastore.getClusters();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      if (!loadBalancerGroups.contains(loadBalancerGroup)) {
        throw new MissingLoadBalancerGroupException(request);
      }
    }
  }

  public BaragonResponse enqueueRequest(BaragonRequest request) throws BasePathConflictException, MissingLoadBalancerGroupException {
    final Optional<BaragonResponse> maybePreexistingResponse = getResponse(request.getLoadBalancerRequestId());

    if (maybePreexistingResponse.isPresent()) {
      return maybePreexistingResponse.get();
    }

    ensureBasePathAvailable(request);
    ensureRequestedLoadBalancersExist(request);

    requestDatastore.addRequest(request);
    requestDatastore.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.SEND_APPLY_REQUESTS);
    requestDatastore.enqueueRequest(request);

    for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
      loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, request.getLoadBalancerService().getServiceBasePath(), request.getLoadBalancerService().getServiceId());
    }

    return new BaragonResponse(request.getLoadBalancerRequestId(), InternalRequestStates.SEND_APPLY_REQUESTS.toRequestState(), Optional.<String>absent());
  }

  public Optional<InternalRequestStates> cancelRequest(String requestId) {
    final Optional<InternalRequestStates> maybeState = getRequestState(requestId);

    if (!maybeState.isPresent() || !maybeState.get().isCancelable()) {
      return maybeState;
    }

    requestDatastore.setRequestState(requestId, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);

    return Optional.of(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);
  }

  public void commitRequest(BaragonRequest request) {
    stateDatastore.addService(request.getLoadBalancerService());
    stateDatastore.removeUpstreams(request.getLoadBalancerService().getServiceId(), request.getRemoveUpstreams());
    stateDatastore.addUpstreams(request.getLoadBalancerRequestId(), request.getLoadBalancerService().getServiceId(), request.getAddUpstreams());
  }
}

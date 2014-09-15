package com.hubspot.baragon.managers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.BasePathConflictException;
import com.hubspot.baragon.exceptions.MissingLoadBalancerGroupException;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.QueuedRequestId;

@Singleton
public class RequestManager {
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonAgentResponseDatastore agentResponseDatastore;

  @Inject
  public RequestManager(BaragonRequestDatastore requestDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore,
                        BaragonStateDatastore stateDatastore, BaragonAgentResponseDatastore agentResponseDatastore) {
    this.requestDatastore = requestDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
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

    return Optional.of(new BaragonResponse(requestId, maybeStatus.get().toRequestState(), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId))));
  }

  private void ensureBasePathAvailable(BaragonRequest request) throws BasePathConflictException {
    final BaragonService service = request.getLoadBalancerService();
    final Map<String, String> loadBalancerServiceIds = Maps.newHashMap();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, service.getServiceBasePath());
      if (maybeServiceId.isPresent() && !maybeServiceId.get().equals(service.getServiceId())) {
        loadBalancerServiceIds.put(loadBalancerGroup, maybeServiceId.get());
      }
    }

    if (!loadBalancerServiceIds.isEmpty()) {
      throw new BasePathConflictException(request, loadBalancerServiceIds);
    }
  }

  private void ensureRequestedLoadBalancersExist(BaragonRequest request) throws MissingLoadBalancerGroupException {
    final BaragonService service = request.getLoadBalancerService();
    final Collection<String> loadBalancerGroups = loadBalancerDatastore.getClusters();

    final Collection<String> missingGroups = Lists.newArrayListWithCapacity(service.getLoadBalancerGroups().size());

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      if (!loadBalancerGroups.contains(loadBalancerGroup)) {
        missingGroups.add(loadBalancerGroup);
      }
    }

    if (!missingGroups.isEmpty()) {
      throw new MissingLoadBalancerGroupException(request, missingGroups);
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

    return new BaragonResponse(request.getLoadBalancerRequestId(), InternalRequestStates.SEND_APPLY_REQUESTS.toRequestState(), Optional.<String>absent(), Optional.<Map<String, Collection<AgentResponse>>>absent());
  }

  public Optional<InternalRequestStates> cancelRequest(String requestId) {
    final Optional<InternalRequestStates> maybeState = getRequestState(requestId);

    if (!maybeState.isPresent() || !maybeState.get().isCancelable()) {
      return maybeState;
    }

    requestDatastore.setRequestState(requestId, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);

    return Optional.of(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);
  }

  public synchronized void commitRequest(BaragonRequest request) {
    final Optional<BaragonService> maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

    // if we've changed the base path, clear out the old ones
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceBasePath().equals(request.getLoadBalancerService().getServiceBasePath())) {
      for (String loadBalancerGroup : maybeOriginalService.get().getLoadBalancerGroups()) {
        loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeOriginalService.get().getServiceBasePath());
      }
    }

    stateDatastore.addService(request.getLoadBalancerService());
    stateDatastore.removeUpstreams(request.getLoadBalancerService().getServiceId(), request.getRemoveUpstreamInfo());
    stateDatastore.addUpstreams(request.getLoadBalancerService().getServiceId(), request.getAddUpstreamInfo());
    stateDatastore.updateStateNode();
  }
}

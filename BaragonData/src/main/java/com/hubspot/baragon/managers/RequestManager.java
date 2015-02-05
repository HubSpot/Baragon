package com.hubspot.baragon.managers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.RequestAlreadyEnqueuedException;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStateDatastore.class);
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

    return Optional.of(new BaragonResponse(requestId, InternalStatesMap.getRequestState(maybeStatus.get()), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId))));
  }

  public Map<String, String> getBasePathConflicts(BaragonRequest request) {
    final BaragonService service = request.getLoadBalancerService();
    final Map<String, String> loadBalancerServiceIds = Maps.newHashMap();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, service.getServiceBasePath());
      if (maybeServiceId.isPresent() && !maybeServiceId.get().equals(service.getServiceId())) {
        if (!request.getReplaceServiceId().isPresent() || (request.getReplaceServiceId().isPresent() && !request.getReplaceServiceId().get().equals(maybeServiceId.get()))) {
          loadBalancerServiceIds.put(loadBalancerGroup, maybeServiceId.get());
        }
      }
    }

    return loadBalancerServiceIds;
  }

  public void revertBasePath(BaragonRequest request) {
    Optional<BaragonService> maybeOriginalService = Optional.absent();
    if (request.getReplaceServiceId().isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getReplaceServiceId().get());
    }
    if (!maybeOriginalService.isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());
    }
    // if the request is not in the state datastore (ie. no previous request) clear the base path lock
    if (!maybeOriginalService.isPresent()) {
      for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
        loadBalancerDatastore.clearBasePath(loadBalancerGroup,  request.getLoadBalancerService().getServiceBasePath());
      }
    }

    // if we changed the base path, revert it to the old one
    if (maybeOriginalService.isPresent() && request.getReplaceServiceId().isPresent() && maybeOriginalService.get().getServiceId().equals(request.getReplaceServiceId().get())) {
      lockBasePaths(request.getLoadBalancerService().getLoadBalancerGroups(), request.getLoadBalancerService().getServiceBasePath(), maybeOriginalService.get().getServiceId());
    }
  }

  public Set<String> getMissingLoadBalancerGroups(BaragonRequest request) {
    final Set<String> groups = new HashSet<>(request.getLoadBalancerService().getLoadBalancerGroups());

    return Sets.difference(groups, loadBalancerDatastore.getLoadBalancerGroups());
  }

  public void lockBasePaths(BaragonRequest request) {
    for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
      loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, request.getLoadBalancerService().getServiceBasePath(), request.getLoadBalancerService().getServiceId());
    }
  }

  public void lockBasePaths(List<String> loadBalancerGroups, String serviceBasePath, String serviceId) {
    for (String loadBalancerGroup : loadBalancerGroups) {
      loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, serviceBasePath, serviceId);
    }
  }

  public BaragonResponse enqueueRequest(BaragonRequest request) throws RequestAlreadyEnqueuedException {
    final Optional<BaragonResponse> maybePreexistingResponse = getResponse(request.getLoadBalancerRequestId());

    if (maybePreexistingResponse.isPresent()) {
      throw new RequestAlreadyEnqueuedException(request.getLoadBalancerRequestId(), maybePreexistingResponse.get());
    }

    requestDatastore.addRequest(request);
    requestDatastore.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.PENDING);

    final QueuedRequestId queuedRequestId = requestDatastore.enqueueRequest(request);

    requestDatastore.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Queued as %s", queuedRequestId));

    return getResponse(request.getLoadBalancerRequestId()).get();
  }

  public Optional<InternalRequestStates> cancelRequest(String requestId) {
    final Optional<InternalRequestStates> maybeState = getRequestState(requestId);

    if (!maybeState.isPresent() || !InternalStatesMap.isCancelable(maybeState.get())) {
      return maybeState;
    }

    requestDatastore.setRequestState(requestId, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);

    return Optional.of(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);
  }

  public synchronized void commitRequest(BaragonRequest request) {
    Optional<BaragonService> maybeOriginalService = Optional.absent();
    if (request.getReplaceServiceId().isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getReplaceServiceId().get());
    }
    if (!maybeOriginalService.isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());
    }

    // if we've changed the base path, clear out the old ones
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceBasePath().equals(request.getLoadBalancerService().getServiceBasePath())) {
      for (String loadBalancerGroup : maybeOriginalService.get().getLoadBalancerGroups()) {
        loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeOriginalService.get().getServiceBasePath());
      }
    }

    //If we have removed a load balancer group, clear the base path for that group
    if (maybeOriginalService.isPresent()) {
      Collection<String> removedLbGroups = maybeOriginalService.get().getLoadBalancerGroups();
      removedLbGroups.removeAll(request.getLoadBalancerService().getLoadBalancerGroups());
      if (!removedLbGroups.isEmpty()) {
        try {
          for (String loadbalancerGroup : removedLbGroups) {
            loadBalancerDatastore.clearBasePath(loadbalancerGroup, maybeOriginalService.get().getServiceBasePath());
          }
        } catch (Exception e) {
          LOG.info(String.format("Error clearing base path %s", e));
        }
      }
    }
    // If the service ID has been changed, remove the old service from the state datastore
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceId().equals(request.getLoadBalancerService().getServiceId())) {
      stateDatastore.removeService(maybeOriginalService.get().getServiceId());
    }

    stateDatastore.addService(request.getLoadBalancerService());
    stateDatastore.removeUpstreams(request.getLoadBalancerService().getServiceId(), request.getRemoveUpstreams());
    stateDatastore.addUpstreams(request.getLoadBalancerService().getServiceId(), request.getAddUpstreams());
    stateDatastore.updateStateNode();

    // if there are no remaining upstreams, clear the base path
    try {
      if (stateDatastore.getUpstreamsMap(request.getLoadBalancerService().getServiceId()).isEmpty()) {
        for (String loadbalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
          loadBalancerDatastore.clearBasePath(loadbalancerGroup, request.getLoadBalancerService().getServiceBasePath());
        }
      }
    } catch (Exception e) {
      LOG.info(String.format("Error clearing base path %s", e));
    }
  }
}

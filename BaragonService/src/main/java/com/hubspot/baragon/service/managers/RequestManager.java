package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonResponseHistoryDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.InvalidRequestActionException;
import com.hubspot.baragon.exceptions.InvalidUpstreamsException;
import com.hubspot.baragon.exceptions.RequestAlreadyEnqueuedException;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.RequestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonAgentResponseDatastore agentResponseDatastore;
  private final BaragonResponseHistoryDatastore responseHistoryDatastore;

  @Inject
  public RequestManager(BaragonRequestDatastore requestDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore,
                        BaragonStateDatastore stateDatastore, BaragonAgentResponseDatastore agentResponseDatastore,
                        BaragonResponseHistoryDatastore responseHistoryDatastore) {
    this.requestDatastore = requestDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.responseHistoryDatastore = responseHistoryDatastore;
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

  public List<BaragonResponse> getResponsesForService(String serviceId) {
    List<BaragonResponse> responses = new ArrayList<>();
    for (String requestId : requestDatastore.getAllRequestIds()) {
      Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
      if (maybeRequest.isPresent() && maybeRequest.get().getLoadBalancerService().getServiceId().equals(serviceId)) {
        Optional<InternalRequestStates> maybeStatus = requestDatastore.getRequestState(requestId);
        if (maybeStatus.isPresent()) {
          responses.add(new BaragonResponse(requestId, InternalStatesMap.getRequestState(maybeStatus.get()), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId)), maybeRequest));
        }
      }
    }
    responses.addAll(responseHistoryDatastore.getResponsesForService(serviceId));
    return responses;
  }

  public Optional<BaragonResponse> getResponse(String requestId) {
    Optional<BaragonResponse> maybeActiveRequestResponse = getResponseFromActiveRequests(requestId);
    if (maybeActiveRequestResponse.isPresent()) {
      return maybeActiveRequestResponse;
    } else {
      Optional<String> maybeServiceId = responseHistoryDatastore.getServiceIdForRequestId(requestId);
      if (maybeServiceId.isPresent()) {
        return responseHistoryDatastore.getResponse(maybeServiceId.get(), requestId);
      } else {
        return Optional.absent();
      }
    }
  }

  public Optional<BaragonResponse> getResponse(String serviceId, String requestId) {
    Optional<BaragonResponse> maybeActiveRequestResponse = getResponseFromActiveRequests(requestId);
    if (maybeActiveRequestResponse.isPresent()) {
      return maybeActiveRequestResponse;
    } else {
      return responseHistoryDatastore.getResponse(serviceId, requestId);
    }
  }

  private Optional<BaragonResponse> getResponseFromActiveRequests(String requestId) {
    if (requestDatastore.activeRequestExists(requestId)) {
      final Optional<InternalRequestStates> maybeStatus = requestDatastore.getRequestState(requestId);

      if (!maybeStatus.isPresent()) {
        return Optional.absent();
      }

      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
      if (!maybeRequest.isPresent()) {
        return Optional.absent();
      }

      return Optional.of(new BaragonResponse(requestId, InternalStatesMap.getRequestState(maybeStatus.get()), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId)), maybeRequest));
    } else {
      return Optional.absent();
    }
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

    return Sets.difference(groups, loadBalancerDatastore.getLoadBalancerGroupNames());
  }

  public void lockBasePaths(BaragonRequest request) {
    for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
      loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, request.getLoadBalancerService().getServiceBasePath(), request.getLoadBalancerService().getServiceId());
    }
  }

  public void lockBasePaths(Set<String> loadBalancerGroups, String serviceBasePath, String serviceId) {
    for (String loadBalancerGroup : loadBalancerGroups) {
      loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, serviceBasePath, serviceId);
    }
  }

  public BaragonResponse enqueueRequest(BaragonRequest request) throws RequestAlreadyEnqueuedException, InvalidRequestActionException, InvalidUpstreamsException {
    final Optional<BaragonResponse> maybePreexistingResponse = getResponse(request.getLoadBalancerService().getServiceId(), request.getLoadBalancerRequestId());

    if (maybePreexistingResponse.isPresent()) {
      Optional<BaragonRequest> maybePreexistingRequest = requestDatastore.getRequest(request.getLoadBalancerRequestId());
      if (maybePreexistingRequest.isPresent() && !maybePreexistingRequest.get().equals(request)) {
        throw new RequestAlreadyEnqueuedException(request.getLoadBalancerRequestId(), maybePreexistingResponse.get(), String.format("Request %s is already enqueued with different parameters", request.getLoadBalancerRequestId()));
      } else {
        return maybePreexistingResponse.get();
      }
    }

    if (!request.getReplaceUpstreams().isEmpty() && (!request.getAddUpstreams().isEmpty() || !request.getRemoveUpstreams().isEmpty())) {
      throw new InvalidUpstreamsException("If overrideUpstreams is specified, addUpstreams and removeUpstreams mustbe empty");
    }

    if (request.getAction().isPresent() && request.getAction().equals(Optional.of(RequestAction.REVERT))) {
      throw new InvalidRequestActionException("The REVERT action may only be used internally by Baragon, you may specify UPDATE, DELETE, RELOAD, or leave the action blank(UPDATE)");
    }

    requestDatastore.addRequest(request);
    requestDatastore.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.PENDING);

    final QueuedRequestId queuedRequestId = requestDatastore.enqueueRequest(request);

    requestDatastore.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Queued as %s", queuedRequestId));

    return getResponse(request.getLoadBalancerService().getServiceId(), request.getLoadBalancerRequestId()).get();
  }

  public Optional<InternalRequestStates> cancelRequest(String requestId) {
    final Optional<InternalRequestStates> maybeState = getRequestState(requestId);

    if (!maybeState.isPresent() || !InternalStatesMap.isCancelable(maybeState.get())) {
      return maybeState;
    }

    requestDatastore.setRequestState(requestId, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);

    return Optional.of(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);
  }

  public void saveResponseToHistory(BaragonRequest request, InternalRequestStates state) {
    BaragonResponse response = new BaragonResponse(request.getLoadBalancerRequestId(), InternalStatesMap.getRequestState(state), requestDatastore.getRequestMessage(request.getLoadBalancerRequestId()), Optional.of(agentResponseDatastore.getLastResponses(request.getLoadBalancerRequestId())), Optional.of(request));
    responseHistoryDatastore.addResponse(request.getLoadBalancerService().getServiceId(), request.getLoadBalancerRequestId(), response);
  }

  public void deleteRequest(String requestId) {
    requestDatastore.deleteRequest(requestId);
  }

  public synchronized void commitRequest(BaragonRequest request) {
    RequestAction action = request.getAction().or(RequestAction.UPDATE);
    Optional<BaragonService> maybeOriginalService = getOriginalService(request);

    switch(action) {
      case UPDATE:
      case REVERT:
        clearChangedBasePaths(request, maybeOriginalService);
        clearBasePathsFromUnusedLbs(request, maybeOriginalService);
        removeOldService(request, maybeOriginalService);
        updateStateDatastore(request);
        clearBasePathsWithNoUpstreams(request);
        break;
      case DELETE:
        clearChangedBasePaths(request, maybeOriginalService);
        clearBasePathsFromUnusedLbs(request, maybeOriginalService);
        deleteRemovedServices(request);
        clearBasePathsWithNoUpstreams(request);
        break;
      default:
        LOG.debug(String.format("No updates to commit for request action %s", action));
        break;
    }
  }

  private Optional<BaragonService> getOriginalService(BaragonRequest request) {
    Optional<BaragonService> maybeOriginalService = Optional.absent();
    if (request.getReplaceServiceId().isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getReplaceServiceId().get());
    }
    if (!maybeOriginalService.isPresent()) {
      maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());
    }
    return maybeOriginalService;
  }

  private void deleteRemovedServices(BaragonRequest request) {
    stateDatastore.removeService(request.getLoadBalancerService().getServiceId());
    if (request.getReplaceServiceId().isPresent() && stateDatastore.getService(request.getReplaceServiceId().get()).isPresent()) {
      stateDatastore.removeService(request.getReplaceServiceId().get());
    }
    stateDatastore.updateStateNode();
  }

  private void updateStateDatastore(BaragonRequest request) {
    try {
      stateDatastore.addService(request.getLoadBalancerService());
      if (!request.getReplaceUpstreams().isEmpty()) {
        stateDatastore.setUpstreams(request.getLoadBalancerService().getServiceId(), request.getReplaceUpstreams());
      } else {
        stateDatastore.removeUpstreams(request.getLoadBalancerService().getServiceId(), request.getRemoveUpstreams());
        stateDatastore.addUpstreams(request.getLoadBalancerService().getServiceId(), request.getAddUpstreams());
      }
      stateDatastore.updateStateNode();
    } catch (Exception e) {
      LOG.error(String.format("Error updating state datastore %s", e));
    }
  }

  private void removeOldService(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceId().equals(request.getLoadBalancerService().getServiceId())) {
      stateDatastore.removeService(maybeOriginalService.get().getServiceId());
    }
  }

  private void clearBasePathsWithNoUpstreams(BaragonRequest request) {
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

  private void clearChangedBasePaths(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceBasePath().equals(request.getLoadBalancerService().getServiceBasePath())) {
      for (String loadBalancerGroup : maybeOriginalService.get().getLoadBalancerGroups()) {
        loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeOriginalService.get().getServiceBasePath());
      }
    }
  }

  private void clearBasePathsFromUnusedLbs(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent()) {
      Set<String> removedLbGroups = maybeOriginalService.get().getLoadBalancerGroups();
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
  }
}

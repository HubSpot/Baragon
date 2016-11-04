package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.config.BaragonConfiguration;

@Singleton
public class RequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(RequestManager.class);
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonAgentResponseDatastore agentResponseDatastore;
  private final BaragonResponseHistoryDatastore responseHistoryDatastore;
  private final BaragonConfiguration configuration;

  @Inject
  public RequestManager(BaragonRequestDatastore requestDatastore,
                        BaragonLoadBalancerDatastore loadBalancerDatastore,
                        BaragonStateDatastore stateDatastore,
                        BaragonAgentResponseDatastore agentResponseDatastore,
                        BaragonResponseHistoryDatastore responseHistoryDatastore,
                        BaragonConfiguration configuration) {
    this.requestDatastore = requestDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.responseHistoryDatastore = responseHistoryDatastore;
    this.configuration = configuration;
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
    responses.addAll(responseHistoryDatastore.getResponsesForService(serviceId, configuration.getHistoryConfiguration().getMaxResponsesToFetch()));
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
      Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(loadBalancerGroup);
      for (String path : service.getAllPaths()) {
        final Optional<String> maybeServiceIdForPath = loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, path);
        if (maybeServiceIdForPath.isPresent() && !maybeServiceIdForPath.get().equals(service.getServiceId())) {
          if (!request.getReplaceServiceId().isPresent() || (request.getReplaceServiceId().isPresent() && !request.getReplaceServiceId().get().equals(maybeServiceIdForPath.get()))) {
            LOG.info("Found base path conflict for {} at path {} with service {}", service.getServiceId(), path, maybeServiceIdForPath.get());
            loadBalancerServiceIds.put(loadBalancerGroup, maybeServiceIdForPath.get());
            continue;
          }
        }
        if (!path.startsWith("/")) {
          if (maybeGroup.isPresent() && maybeGroup.get().getDefaultDomain().isPresent() && path.startsWith(maybeGroup.get().getDefaultDomain().get())) {
            Optional<String> maybeServiceForDefaultDomainPath = loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, path.replace(maybeGroup.get().getDefaultDomain().get(), ""));
            if (maybeServiceForDefaultDomainPath.isPresent() && !maybeServiceForDefaultDomainPath.get().equals(service.getServiceId())) {
              if (!request.getReplaceServiceId().isPresent() || (request.getReplaceServiceId().isPresent() && !request.getReplaceServiceId().get().equals(maybeServiceForDefaultDomainPath.get()))) {
                LOG.info("Found base path conflict for {} at path {} with service {}", service.getServiceId(),path.replace(maybeGroup.get().getDefaultDomain().get(), ""), maybeServiceForDefaultDomainPath.get());
                loadBalancerServiceIds.put(loadBalancerGroup, maybeServiceForDefaultDomainPath.get());
              }
            }
          }
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
        for (String path : request.getLoadBalancerService().getAllPaths()) {
          loadBalancerDatastore.clearBasePath(loadBalancerGroup, path);
        }
      }
    }

    // if we changed the base path, revert it to the old one
    if (maybeOriginalService.isPresent() && request.getReplaceServiceId().isPresent() && maybeOriginalService.get().getServiceId().equals(request.getReplaceServiceId().get())) {
      lockBasePaths(request.getLoadBalancerService().getLoadBalancerGroups(), request.getLoadBalancerService().getAllPaths(), maybeOriginalService.get().getServiceId());
    }
  }

  public Set<String> getMissingLoadBalancerGroups(BaragonRequest request) {
    final Set<String> groups = new HashSet<>(request.getLoadBalancerService().getLoadBalancerGroups());

    return Sets.difference(groups, loadBalancerDatastore.getLoadBalancerGroupNames());
  }

  public void lockBasePaths(BaragonRequest request) {
    for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
      for (String path : request.getLoadBalancerService().getAllPaths()) {
        loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, path, request.getLoadBalancerService().getServiceId());
      }
    }
  }

  public void lockBasePaths(Set<String> loadBalancerGroups, List<String> paths, String serviceId) {
    for (String loadBalancerGroup : loadBalancerGroups) {
      for (String path : paths) {
        loadBalancerDatastore.setBasePathServiceId(loadBalancerGroup, path, serviceId);
      }
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

    if (request.isNoReload() && request.getAction().isPresent() && request.getAction().get().equals(RequestAction.RELOAD)) {
      throw new InvalidRequestActionException("You can not specify 'noReload' on a request with action 'RELOAD'");
    }

    if (!request.getReplaceUpstreams().isEmpty() && (!request.getAddUpstreams().isEmpty() || !request.getRemoveUpstreams().isEmpty())) {
      throw new InvalidUpstreamsException("If overrideUpstreams is specified, addUpstreams and removeUpstreams mustbe empty");
    }

    if (request.getAction().isPresent() && request.getAction().equals(Optional.of(RequestAction.REVERT))) {
      throw new InvalidRequestActionException("The REVERT action may only be used internally by Baragon, you may specify UPDATE, DELETE, RELOAD, or leave the action blank(UPDATE)");
    }

    final QueuedRequestId queuedRequestId = requestDatastore.enqueueRequest(request, InternalRequestStates.PENDING);

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

  public synchronized void commitRequest(BaragonRequest request) throws Exception {
    RequestAction action = request.getAction().or(RequestAction.UPDATE);
    Optional<BaragonService> maybeOriginalService = getOriginalService(request);

    switch(action) {
      case UPDATE:
      case REVERT:
        updateStateDatastore(request);
        clearChangedBasePaths(request, maybeOriginalService);
        clearBasePathsFromUnusedLbs(request, maybeOriginalService);
        removeOldService(request, maybeOriginalService);
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
    updateLastRequestForGroups(request);
  }

  private void updateLastRequestForGroups(BaragonRequest request) {
    for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
      loadBalancerDatastore.setLastRequestId(loadBalancerGroup, request.getLoadBalancerRequestId());
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
    stateDatastore.incrementStateVersion();
  }

  private void updateStateDatastore(BaragonRequest request) throws Exception {
    stateDatastore.updateService(request);
    try {
      stateDatastore.incrementStateVersion();
    } catch (Exception e) {
      LOG.error("Error updating state datastore", e);
    }
  }

  private void removeOldService(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent() && !maybeOriginalService.get().getServiceId().equals(request.getLoadBalancerService().getServiceId())) {
      stateDatastore.removeService(maybeOriginalService.get().getServiceId());
    }
  }

  private void clearBasePathsWithNoUpstreams(BaragonRequest request) {
    try {
      if (stateDatastore.getUpstreams(request.getLoadBalancerService().getServiceId()).isEmpty()) {
        for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
          for (String path : request.getLoadBalancerService().getAllPaths()) {
            if (loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, path).or("").equals(request.getLoadBalancerService().getServiceId())) {
              loadBalancerDatastore.clearBasePath(loadBalancerGroup, path);
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error clearing base path", e);
    }
  }

  private void clearChangedBasePaths(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent()) {
      try {
        List<String> newPaths = request.getLoadBalancerService().getAllPaths();
        for (String oldPath : maybeOriginalService.get().getAllPaths()) {
          if (!newPaths.contains(oldPath)) {
            for (String loadBalancerGroup : maybeOriginalService.get().getLoadBalancerGroups()) {
              if (loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, oldPath).or("").equals(maybeOriginalService.get().getServiceId())) {
                loadBalancerDatastore.clearBasePath(loadBalancerGroup, oldPath);
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Error clearing base path", e);
      }
    }
  }

  private void clearBasePathsFromUnusedLbs(BaragonRequest request, Optional<BaragonService> maybeOriginalService) {
    if (maybeOriginalService.isPresent()) {
      Set<String> removedLbGroups = maybeOriginalService.get().getLoadBalancerGroups();
      removedLbGroups.removeAll(request.getLoadBalancerService().getLoadBalancerGroups());
      if (!removedLbGroups.isEmpty()) {
        try {
          for (String loadBalancerGroup : removedLbGroups) {
            for (String path : maybeOriginalService.get().getAllPaths()) {
              if (loadBalancerDatastore.getBasePathServiceId(loadBalancerGroup, path).or("").equals(maybeOriginalService.get().getServiceId())) {
                loadBalancerDatastore.clearBasePath(loadBalancerGroup, path);
              }
            }
          }
        } catch (Exception e) {
          LOG.error("Error clearing base path", e);
        }
      }
    }
  }
}

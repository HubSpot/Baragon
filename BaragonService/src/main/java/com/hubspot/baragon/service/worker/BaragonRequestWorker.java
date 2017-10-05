package com.hubspot.baragon.service.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentRequestsStatus;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.QueuedRequestWithState;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.utils.JavaUtils;

@Singleton
public class BaragonRequestWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonRequestWorker.class);

  private final AgentManager agentManager;
  private final RequestManager requestManager;
  private final AtomicLong workerLastStartAt;
  private final BaragonExceptionNotifier exceptionNotifier;
  private final BaragonConfiguration configuration;
  private final EdgeCache edgeCache;

  @Inject
  public BaragonRequestWorker(AgentManager agentManager,
                              RequestManager requestManager,
                              BaragonExceptionNotifier exceptionNotifier,
                              BaragonConfiguration configuration,
                              EdgeCache edgeCache,
                              @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.agentManager = agentManager;
    this.requestManager = requestManager;
    this.edgeCache = edgeCache;
    this.workerLastStartAt = workerLastStartAt;
    this.exceptionNotifier = exceptionNotifier;
    this.configuration = configuration;
  }

  private String buildResponseString(Map<String, Collection<AgentResponse>> agentResponses, AgentRequestType requestType) {
    if (agentResponses.containsKey(requestType.name()) && !agentResponses.get(requestType.name()).isEmpty()) {
      Set<String> messages = new HashSet<>();
      for (AgentResponse response : agentResponses.get(requestType.name())) {
        if (response.toRequestStatus() == AgentRequestsStatus.FAILURE || response.toRequestStatus() == AgentRequestsStatus.INVALID_REQUEST_NOOP) {
          messages.add(String.format("(%s) - %s", response.getStatusCode().or(0), response.getContent().or(response.getException()).or("")));
        } else {
          messages.add(String.format("%s - %s", response.getUrl(), response.toRequestStatus().name()));
        }
      }
      return JavaUtils.COMMA_JOINER.join(messages);
    } else {
      return "No agent responses";
    }
  }

  private InternalRequestStates handleCheckRevertResponse(BaragonRequest request, InternalRequestStates currentState) {
    final Map<String, Collection<AgentResponse>> agentResponses;

    switch (agentManager.getRequestsStatus(request, InternalStatesMap.getRequestType(currentState))) {
      case FAILURE:
        agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed {%s}, %s failed {%s}", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap.getRequestType(currentState).name(), buildResponseString(agentResponses, InternalStatesMap.getRequestType(currentState))));
        return InternalStatesMap.getFailureState(currentState);
      case SUCCESS:
        agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed {%s}, %s OK.", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap.getRequestType(currentState).name()));
        requestManager.revertBasePath(request);
        return InternalStatesMap.getSuccessState(currentState);
      case RETRY:
        return InternalStatesMap.getRetryState(currentState);
      default:
        return InternalStatesMap.getWaitingState(currentState);
    }
  }

  private InternalRequestStates handleState(InternalRequestStates currentState, BaragonRequest request) {
    switch (currentState) {
      case PENDING:
        final Map<String, String> conflicts = requestManager.getBasePathConflicts(request);

        if (!conflicts.isEmpty() && !(request.getAction().or(RequestAction.UPDATE) == RequestAction.DELETE)) {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), getBasePathConflictMessage(conflicts));
          return InternalRequestStates.INVALID_REQUEST_NOOP;
        }

        final Set<String> missingGroups = requestManager.getMissingLoadBalancerGroups(request);

        if (!missingGroups.isEmpty()) {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Invalid request due to non-existent load balancer groups: %s", missingGroups));
          return InternalRequestStates.INVALID_REQUEST_NOOP;
        }

        for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
          if (agentManager.invalidAgentCount(loadBalancerGroup)) {
            requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Invalid request due to not enough agents present for group: %s", loadBalancerGroup));
            return InternalRequestStates.FAILED_REVERTED;
          }
        }

        if (!request.getLoadBalancerService().getDomains().isEmpty()) {
          List<String> domainsNotServed = getDomainsNotServed(request.getLoadBalancerService());
          if (!domainsNotServed.isEmpty()) {
            requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("No groups present that serve domains: %s", domainsNotServed));
            return  InternalRequestStates.INVALID_REQUEST_NOOP;
          }
        }

        if (!(request.getAction().or(RequestAction.UPDATE) == RequestAction.DELETE)) {
          requestManager.lockBasePaths(request);
        }

        return InternalRequestStates.SEND_APPLY_REQUESTS;

      case CHECK_APPLY_RESPONSES:
        switch (agentManager.getRequestsStatus(request, InternalStatesMap.getRequestType(currentState))) {
          case FAILURE:
            final Map<String, Collection<AgentResponse>> agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
            requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), reverting...", buildResponseString(agentResponses, InternalStatesMap.getRequestType(currentState))));
            return InternalRequestStates.FAILED_SEND_REVERT_REQUESTS;
          case SUCCESS:
            try {
              requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("%s request succeeded! Added upstreams: %s, Removed upstreams: %s", request.getAction().or(RequestAction.UPDATE), request.getAddUpstreams(), request.getRemoveUpstreams()));
              requestManager.commitRequest(request);
              performPostApplySteps(request);
              return InternalRequestStates.COMPLETED;
            } catch (KeeperException ke) {
              String message = String.format("Caught zookeeper error for path %s.", ke.getPath());
              LOG.error(message, ke);
              requestManager.setRequestMessage(request.getLoadBalancerRequestId(), message + ke.getMessage());
              return InternalRequestStates.FAILED_SEND_REVERT_REQUESTS;
            } catch (Exception e) {
              LOG.warn(String.format("Request %s was successful, but failed to commit!", request.getLoadBalancerRequestId()), e);
              exceptionNotifier.notify(e, ImmutableMap.of("requestId", request.getLoadBalancerRequestId(), "serviceId", request.getLoadBalancerService().getServiceId()));
              return InternalRequestStates.FAILED_SEND_REVERT_REQUESTS;
            }
          case RETRY:
            return InternalRequestStates.SEND_APPLY_REQUESTS;
          case INVALID_REQUEST_NOOP:
            requestManager.revertBasePath(request);
            return InternalRequestStates.INVALID_REQUEST_NOOP;
          default:
            return InternalRequestStates.CHECK_APPLY_RESPONSES;
        }

      case SEND_APPLY_REQUESTS:
      case FAILED_SEND_REVERT_REQUESTS:
      case CANCELLED_SEND_REVERT_REQUESTS:
        throw new RuntimeException(String.format("Requests in state %s must be handled by batch request sender", currentState));

      case FAILED_CHECK_REVERT_RESPONSES:
      case CANCELLED_CHECK_REVERT_RESPONSES:
        return handleCheckRevertResponse(request, currentState);

      default:
        return currentState;
    }
  }

  private void performPostApplySteps(BaragonRequest request) {
    if (configuration.getEdgeCacheConfiguration().isEnabled() &&
        edgeCache.invalidateIfNecessary(request)) {
      LOG.info("Invalidated edge cache for {}", request);
    }
  }

  private List<String> getDomainsNotServed(BaragonService service) {
    List<String> notServed = new ArrayList<>(service.getDomains());
    for (String group : service.getLoadBalancerGroups()) {
      Set<String> domains = agentManager.getAllDomainsForGroup(group);
      for (String domain : domains) {
        notServed.remove(domain);
      }
    }
    return notServed;
  }

  private String getBasePathConflictMessage(Map<String, String> conflicts) {
    String message = "Invalid request due to base path conflicts: [";
    for (Map.Entry<String, String> entry : conflicts.entrySet()) {
      message = String.format("%s %s on group %s,", message, entry.getValue(), entry.getKey());
    }
    return message.substring(0, message.length() -1) + " ]";
  }

  public Map<QueuedRequestWithState, InternalRequestStates> handleQueuedRequests(Set<QueuedRequestWithState> queuedRequestsWithState) {
    Map<QueuedRequestWithState, InternalRequestStates> results = new HashMap<>();
    Set<QueuedRequestWithState> toApply = Sets.newHashSet();
    for (QueuedRequestWithState queuedRequestWithState : queuedRequestsWithState) {
      if (!queuedRequestWithState.getCurrentState().isRequireAgentRequest()) {
        try {
          results.put(queuedRequestWithState, handleState(queuedRequestWithState.getCurrentState(), queuedRequestWithState.getRequest()));
        } catch (Exception e) {
          LOG.error("Error processing request {}", queuedRequestWithState.getRequest().getLoadBalancerRequestId(), e);
        }
      } else {
        if (toApply.size() < configuration.getWorkerConfiguration().getMaxBatchSize()) {
          toApply.add(queuedRequestWithState);
        }
      }
    }
    results.putAll(agentManager.sendRequests(toApply));
    return results;
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());

    try {
      final List<QueuedRequestId> queuedRequestIds = requestManager.getQueuedRequestIds();

      if (!queuedRequestIds.isEmpty()) {
        final Set<String> handledServices = Sets.newHashSet();
        final Set<QueuedRequestWithState> queuedRequestsWithState = Sets.newHashSet();
        for (QueuedRequestId queuedRequestId : queuedRequestIds) {
          if (!handledServices.contains(queuedRequestId.getServiceId())) {
            final String requestId = queuedRequestId.getRequestId();
            final Optional<InternalRequestStates> maybeState = requestManager.getRequestState(requestId);

            if (!maybeState.isPresent()) {
              LOG.warn(String.format("%s does not have a request status!", requestId));
              continue;
            }

            final Optional<BaragonRequest> maybeRequest = requestManager.getRequest(requestId);

            if (!maybeRequest.isPresent()) {
              LOG.warn(String.format("%s does not have a request object!", requestId));
              continue;
            }

            queuedRequestsWithState.add(new QueuedRequestWithState(queuedRequestId, maybeRequest.get(), maybeState.get()));
            handledServices.add(queuedRequestId.getServiceId());
          }
        }

        Map<QueuedRequestWithState, InternalRequestStates> results = handleQueuedRequests(queuedRequestsWithState);

        for (Map.Entry<QueuedRequestWithState, InternalRequestStates> result : results.entrySet()) {
          if (result.getValue() != result.getKey().getCurrentState()) {
            LOG.info(String.format("%s: %s --> %s", result.getKey().getQueuedRequestId().getRequestId(), result.getKey().getCurrentState(), result.getValue()));
            requestManager.setRequestState(result.getKey().getQueuedRequestId().getRequestId(), result.getValue());
          }

          if (InternalStatesMap.isRemovable(result.getValue())) {
            requestManager.removeQueuedRequest(result.getKey().getQueuedRequestId());
            requestManager.saveResponseToHistory(result.getKey().getRequest(), result.getValue());
            requestManager.deleteRequest(result.getKey().getQueuedRequestId().getRequestId());
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Caught exception", e);
      exceptionNotifier.notify(e, Collections.<String, String>emptyMap());
    }
  }
}

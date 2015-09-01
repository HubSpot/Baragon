package com.hubspot.baragon.service.worker;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.utils.JavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BaragonRequestWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonRequestWorker.class);

  private final AgentManager agentManager;
  private final RequestManager requestManager;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonRequestWorker(AgentManager agentManager,
                              RequestManager requestManager,
                              @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.agentManager = agentManager;
    this.requestManager = requestManager;
    this.workerLastStartAt = workerLastStartAt;
  }

  private String buildResponseString(Map<String, Collection<AgentResponse>> agentResponses, AgentRequestType requestType) {
    if (agentResponses.containsKey(requestType.name())) {
      return JavaUtils.COMMA_JOINER.join(agentResponses.get(requestType.name()));
    } else {
      return "no responses";
    }
  }

  private InternalRequestStates handleCheckRevertResponse(BaragonRequest request, InternalRequestStates currentState) {
    final Map<String, Collection<AgentResponse>> agentResponses;

    switch (agentManager.getRequestsStatus(request, InternalStatesMap.getRequestType(currentState))) {
      case FAILURE:
        agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), %s failed (%s)", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap.getRequestType(currentState).name(), buildResponseString(agentResponses, InternalStatesMap.getRequestType(currentState))));
        return InternalStatesMap.getFailureState(currentState);
      case SUCCESS:
        agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), %s OK.", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap.getRequestType(currentState).name()));
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

        if (!conflicts.isEmpty()) {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Invalid request due to base path conflicts: %s", conflicts));
          return InternalRequestStates.INVALID_REQUEST_NOOP;
        }

        final Set<String> missingGroups = requestManager.getMissingLoadBalancerGroups(request);

        if (!missingGroups.isEmpty()) {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Invalid request due to non-existent load balancer groups: %s", missingGroups));
          return InternalRequestStates.INVALID_REQUEST_NOOP;
        }

//        for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
//          if (agentManager.hasNoAgents(loadBalancerGroup)) {
//            requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Invalid request due to no agents present for group: %s", loadBalancerGroup));
//            return InternalRequestStates.INVALID_REQUEST_NOOP;
//          }
//        }

        requestManager.lockBasePaths(request);

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
              return InternalRequestStates.COMPLETED;
            } catch (Exception e) {
              LOG.warn(String.format("Request %s was successful, but failed to commit!", request.getLoadBalancerRequestId()), e);
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
        agentManager.sendRequests(request, InternalStatesMap.getRequestType(currentState));
        return InternalStatesMap.getWaitingState(currentState);

      case FAILED_CHECK_REVERT_RESPONSES:
      case CANCELLED_CHECK_REVERT_RESPONSES:
        return handleCheckRevertResponse(request, currentState);

      default:
        return currentState;
    }
  }

  public void handleQueuedRequest(QueuedRequestId queuedRequestId) {
    final String requestId = queuedRequestId.getRequestId();

    final Optional<InternalRequestStates> maybeState = requestManager.getRequestState(requestId);

    if (!maybeState.isPresent()) {
      LOG.warn(String.format("%s does not have a request status!", requestId));
      return;
    }

    final InternalRequestStates currentState = maybeState.get();

    final Optional<BaragonRequest> maybeRequest = requestManager.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      LOG.warn(String.format("%s does not have a request object!", requestId));
      return;
    }

    final InternalRequestStates newState = handleState(currentState, maybeRequest.get());

    if (newState != currentState) {
      LOG.info(String.format("%s: %s --> %s", requestId, currentState, newState));
      requestManager.setRequestState(requestId, newState);
    }

    if (InternalStatesMap.isRemovable(newState)) {
      requestManager.removeQueuedRequest(queuedRequestId);
      requestManager.saveResponseToHistory(maybeRequest.get(), newState);
      requestManager.deleteRequest(requestId);
    }
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());

    try {

      final List<QueuedRequestId> queuedRequestIds = requestManager.getQueuedRequestIds();

      if (!queuedRequestIds.isEmpty()) {
        final Set<String> handledServices = Sets.newHashSet();  // only handle one request per service at a time

        for (QueuedRequestId queuedRequestId : queuedRequestIds) {
          if (!handledServices.contains(queuedRequestId.getServiceId())) {
            synchronized (BaragonRequestWorker.class) {
              handleQueuedRequest(queuedRequestId);
            }
            handledServices.add(queuedRequestId.getServiceId());
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Caught exception", e);
    }
  }
}

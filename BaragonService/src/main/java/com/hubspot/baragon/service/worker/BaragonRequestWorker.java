package com.hubspot.baragon.service.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonStateDatastore;
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
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.utils.JavaUtils;
import com.hubspot.baragon.utils.UpstreamResolver;

@Singleton
public class BaragonRequestWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonRequestWorker.class);

  private final AgentManager agentManager;
  private final RequestManager requestManager;
  private final BaragonStateDatastore stateDatastore;
  private final AtomicLong workerLastStartAt;
  private final BaragonExceptionNotifier exceptionNotifier;
  private final BaragonConfiguration configuration;
  private final EdgeCache edgeCache;
  private final UpstreamResolver resolver;
  private final ReentrantLock lock;

  @Inject
  public BaragonRequestWorker(AgentManager agentManager,
                              RequestManager requestManager,
                              BaragonStateDatastore stateDatastore,
                              BaragonExceptionNotifier exceptionNotifier,
                              BaragonConfiguration configuration,
                              EdgeCache edgeCache,
                              UpstreamResolver resolver,
                              @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStartAt,
                              @Named(BaragonServiceModule.REQUEST_LOCK) ReentrantLock lock) {
    this.agentManager = agentManager;
    this.requestManager = requestManager;
    this.stateDatastore = stateDatastore;
    this.resolver = resolver;
    this.edgeCache = edgeCache;
    this.workerLastStartAt = workerLastStartAt;
    this.exceptionNotifier = exceptionNotifier;
    this.configuration = configuration;
    this.lock = lock;
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
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed {%s}, %s failed {%s}", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap
            .getRequestType(currentState)
            .name(), buildResponseString(agentResponses, InternalStatesMap.getRequestType(currentState))));
        return InternalStatesMap.getFailureState(currentState);
      case SUCCESS:
        agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed {%s}, %s OK.", buildResponseString(agentResponses, AgentRequestType.APPLY), InternalStatesMap.getRequestType(currentState)
            .name()));
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
            return InternalRequestStates.INVALID_REQUEST_NOOP;
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
              requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("%s request succeeded! Added upstreams: %s, Removed upstreams: %s", request.getAction()
                  .or(RequestAction.UPDATE), request.getAddUpstreams(), request.getRemoveUpstreams()));
              requestManager.commitRequest(request);
              if (performPostApplySteps(request)) {
                return InternalRequestStates.COMPLETED;
              } else {
                return InternalRequestStates.COMPLETED_POST_APPLY_FAILED;
              }
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

  private boolean performPostApplySteps(BaragonRequest request) {
    if (configuration.getEdgeCacheConfiguration().isEnabled()) {
      if (edgeCache.invalidateIfNecessary(request)) {
        LOG.info("Invalidated edge cache for {}", request);
        return true;
      } else {
        return false;
      }
    }
    return true;
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
    return message.substring(0, message.length() - 1) + " ]";
  }

  private Map<QueuedRequestWithState, InternalRequestStates> handleQueuedRequests(List<QueuedRequestWithState> queuedRequestsWithState) {
    Map<QueuedRequestWithState, InternalRequestStates> results = new HashMap<>();
    List<QueuedRequestWithState> toApply = new ArrayList<>();
    for (QueuedRequestWithState queuedRequestWithState : queuedRequestsWithState) {
      LOG.debug("Handling {}", queuedRequestsWithState);
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
    lock.lock();
    workerLastStartAt.set(System.currentTimeMillis());
    try {
      final List<QueuedRequestWithState> queuedRequests = requestManager.getQueuedRequestIds()
          .stream()
          .map(this::hydrateQueuedRequestWithState)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toList());

      List<QueuedRequestWithState> inFlightRequests = queuedRequests.stream()
          .filter((q) -> q.getCurrentState().isInFlight() || hasInProgressAttempt(q))
          .collect(Collectors.toList());

      final Set<String> inProgressServices = inFlightRequests.stream()
          .map((q) -> q.getQueuedRequestId().getServiceId())
          .collect(Collectors.toSet());

      final Set<QueuedRequestWithState> removedForCurrentInFlightRequest = queuedRequests.stream()
          .filter((q) -> inProgressServices.contains(q.getQueuedRequestId().getServiceId()) && !inFlightRequests.contains(q))
          .collect(Collectors.toSet());
      if (!inFlightRequests.isEmpty()) {
        LOG.info("Skipping new updates for services {} due to current in-flight updates", String.join(",", inProgressServices));
      }
      queuedRequests.removeAll(removedForCurrentInFlightRequest);

      // First process results for any requests that were already in-flight
      LOG.debug("Processing {} BaragonRequests which are already in-flight", inFlightRequests.size());
      handleResultStates(handleQueuedRequests(inFlightRequests));
      queuedRequests.removeAll(inFlightRequests);

      int added = inFlightRequests.size();

      while (added < configuration.getWorkerConfiguration().getMaxRequestsPerPoll() && !queuedRequests.isEmpty()) {
        ArrayList<QueuedRequestWithState> nonServiceChanges = new ArrayList<>();
        ArrayList<QueuedRequestWithState> serviceChanges = new ArrayList<>();

        // Build the batches of requests to be sent to agents
        collectRequests(added, queuedRequests, nonServiceChanges, serviceChanges);

        // Now take the list of non-service-change requests,
        // and sort them such that the quicker noValidate / noReload requests come first.
        List<QueuedRequestWithState> hydratedNonServiceChanges = nonServiceChanges.stream()
            .filter((q) -> {
              // Filter here as well since the Set has changed by the second run of the while loop
              if (inProgressServices.contains(q.getQueuedRequestId().getServiceId())) {
                LOG.info("Skipping {} because {} already has an in progress request", q.getQueuedRequestId().getRequestId(), q.getQueuedRequestId().getServiceId());
                return false;
              }
              return true;
            })
            .map(someRequest -> new MaybeAdjustedRequest(someRequest, false))
            .map(this::setNoValidateIfRequestRemovesUpstreamsOnly)
            .map(this::preResolveDNS)
            .map(this::saveAdjustedRequest)
            .sorted(queuedRequestComparator())
            .collect(Collectors.toList());

        added += hydratedNonServiceChanges.size();

        // Then send them off.
        LOG.debug("Processing {} BaragonRequests which don't modify a BaragonService", nonServiceChanges.size());
        handleResultStates(handleQueuedRequests(hydratedNonServiceChanges));

        queuedRequests.removeAll(nonServiceChanges);
        inProgressServices.addAll(hydratedNonServiceChanges.stream().map((q) -> q.getQueuedRequestId().getServiceId()).collect(Collectors.toSet()));

        // Now send off the service change requests, after filtering for services already in flight
        List<QueuedRequestWithState> hydratedServiceChanges = serviceChanges.stream()
            .filter((q) -> {
              if (inProgressServices.contains(q.getQueuedRequestId().getServiceId())) {
                LOG.info("Skipping {} because {} already has an in progress request", q.getQueuedRequestId().getRequestId(), q.getQueuedRequestId().getServiceId());
                return false;
              }
              return true;
            })
            .sorted(queuedRequestComparator())
            .collect(Collectors.toList());

        added += hydratedServiceChanges.size();

        LOG.debug("Processing {} BaragonRequests which modify a BaragonService", serviceChanges.size());
        handleResultStates(handleQueuedRequests(hydratedServiceChanges));

        queuedRequests.removeAll(serviceChanges);
        inProgressServices.addAll(hydratedServiceChanges.stream().map((q) -> q.getQueuedRequestId().getServiceId()).collect(Collectors.toSet()));

        // ...and repeat until we've processed up to the limit of requests
      }
    } catch (Exception e) {
      LOG.warn("Caught exception", e);
      exceptionNotifier.notify(e, Collections.emptyMap());
    } finally {
      lock.unlock();
    }
  }

  /*
   * InternalRequestStates is not marked as in-progress so that new requests will fire. It can also be in this
   * state when retry requests need to fire due to a failure to update a single agent. It should still be
   * marked as in-progress in this case
   */
  private boolean hasInProgressAttempt(QueuedRequestWithState queuedRequestWithState) {
    return queuedRequestWithState.getCurrentState() == InternalRequestStates.SEND_APPLY_REQUESTS &&
        !agentManager.getAgentResponses(queuedRequestWithState.getQueuedRequestId().getRequestId()).isEmpty();
  }

  private static class MaybeAdjustedRequest {
    QueuedRequestWithState request;
    boolean wasAdjusted;

    private MaybeAdjustedRequest(QueuedRequestWithState request, boolean wasAdjusted) {
      this.request = request;
      this.wasAdjusted = wasAdjusted;
    }
  }

  private QueuedRequestWithState saveAdjustedRequest(MaybeAdjustedRequest maybeAdjustedRequest) {
    if (maybeAdjustedRequest.wasAdjusted) {
      try {
        requestManager.updateRequest(maybeAdjustedRequest.request.getRequest());
      } catch (Exception e) {
        // This is just an optimization, so don't blow up if it fails.
        LOG.warn("Unable to save adjustments for request {}", maybeAdjustedRequest.request.getQueuedRequestId().getRequestId(), e);
      }
    }

    return maybeAdjustedRequest.request;
  }

  private MaybeAdjustedRequest setNoValidateIfRequestRemovesUpstreamsOnly(MaybeAdjustedRequest nonServiceChangeRequest) {
    BaragonRequest originalRequest = nonServiceChangeRequest.request.getRequest();

    if (nonServiceChangeRequest.request.getRequest().isNoValidate()) {
      return nonServiceChangeRequest;
    }

    boolean upstreamRemovalsOnly = !originalRequest.getRemoveUpstreams().isEmpty()
        && originalRequest.getAddUpstreams().isEmpty()
        && originalRequest.getReplaceUpstreams().isEmpty();

    if (upstreamRemovalsOnly) {
      LOG.trace("Request {} does not change a BaragonService and only removes upstreams. Setting noValidate.", nonServiceChangeRequest.request.getQueuedRequestId().getRequestId());
      // This BaragonRequest doesn't change the associated BaragonService, and only removes upstreams. We can skip the config check on the nginx side.
      BaragonRequest requestWithNoValidate = originalRequest.toBuilder().setNoValidate(true).build();

      return new MaybeAdjustedRequest(new QueuedRequestWithState(
          nonServiceChangeRequest.request.getQueuedRequestId(),
          requestWithNoValidate,
          nonServiceChangeRequest.request.getCurrentState()
      ), true);
    }

    return nonServiceChangeRequest;
  }

  private MaybeAdjustedRequest preResolveDNS(MaybeAdjustedRequest nonServiceChangeRequest) {
    if (!nonServiceChangeRequest.request.getRequest().getLoadBalancerService().isPreResolveUpstreamDNS()) {
      return nonServiceChangeRequest;
    }

    BaragonRequest originalRequest = nonServiceChangeRequest.request.getRequest();

    List<UpstreamInfo> maybeResolvedAddUpstreams = resolveDNSForAllUpstreams(nonServiceChangeRequest.request.getRequest().getAddUpstreams());
    List<UpstreamInfo> maybeResolvedRemoveUpstreams = resolveDNSForAllUpstreams(nonServiceChangeRequest.request.getRequest().getRemoveUpstreams());
    List<UpstreamInfo> maybeResolvedReplaceUpstreams = resolveDNSForAllUpstreams(nonServiceChangeRequest.request.getRequest().getReplaceUpstreams());

    if (allUpstreamsAreResolved(maybeResolvedAddUpstreams)
        && allUpstreamsAreResolved(maybeResolvedRemoveUpstreams)
        && allUpstreamsAreResolved(maybeResolvedReplaceUpstreams)) {
      LOG.trace("Request {} does not change a BaragonService and all upstreams were pre-resolved. Setting noValidate.", nonServiceChangeRequest.request.getQueuedRequestId().getRequestId());
      BaragonRequest requestWithResolvedUpstreams = originalRequest.toBuilder()
          .setAddUpstreams(maybeResolvedAddUpstreams)
          .setRemoveUpstreams(maybeResolvedRemoveUpstreams)
          .setReplaceUpstreams(maybeResolvedReplaceUpstreams)
          .setNoValidate(true)
          .build();

      return new MaybeAdjustedRequest(new QueuedRequestWithState(
          nonServiceChangeRequest.request.getQueuedRequestId(),
          requestWithResolvedUpstreams,
          nonServiceChangeRequest.request.getCurrentState()
      ), true);
    }

    return nonServiceChangeRequest;
  }

  private boolean allUpstreamsAreResolved(Collection<UpstreamInfo> upstreams) {
    if (upstreams.isEmpty()) {
      return true;
    }

    return upstreams.stream().allMatch(upstreamInfo -> upstreamInfo.getResolvedUpstream().isPresent());
  }

  private List<UpstreamInfo> resolveDNSForAllUpstreams(Collection<UpstreamInfo> upstreams) {
    return upstreams.stream()
        .map(upstreamInfo -> new UpstreamInfo(
            upstreamInfo.getUpstream(),
            upstreamInfo.getRequestId(), upstreamInfo.getRackId(),
            upstreamInfo.getOriginalPath(),
            Optional.fromNullable(upstreamInfo.getGroup()),
            resolver.resolveUpstreamDNS(upstreamInfo.getUpstream())
        ))
        .collect(Collectors.toList());
  }

  private void handleResultStates(Map<QueuedRequestWithState, InternalRequestStates> results) {
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

  private void collectRequests(int previouslyAdded,
                              List<QueuedRequestWithState> queuedRequests,
                              ArrayList<QueuedRequestWithState> nonServiceChanges,
                              ArrayList<QueuedRequestWithState> serviceChanges) {
    int added = previouslyAdded;
    Set<String> boundaryServices = new HashSet<>();

    for (QueuedRequestWithState queuedRequest : queuedRequests) {
      if (added >= configuration.getWorkerConfiguration().getMaxRequestsPerPoll()) {
        return;
      }

      if (boundaryServices.contains(queuedRequest.getQueuedRequestId().getServiceId())) {
        continue;
      }

      // Grab as many non-service-change BaragonRequests as we can.
      if (requestManager.getRequest(queuedRequest.getQueuedRequestId().getRequestId()).transform(someRequest -> !isBatchBoundary(someRequest)).or(false)) {
        nonServiceChanges.add(queuedRequest);
        added++;
      } else {
        // Once we hit a BaragonRequest that specifies BaragonService changes, stop collecting requests for this service.
        serviceChanges.add(queuedRequest);
        boundaryServices.add(queuedRequest.getQueuedRequestId().getServiceId());
        added++;
      }
    }
    return;
  }

  private boolean isBatchBoundary(BaragonRequest baragonRequest) {
    return !stateDatastore.isServiceUnchanged(baragonRequest) || !baragonRequest.getAction().or(RequestAction.UPDATE).equals(RequestAction.UPDATE);
  }

  private Optional<QueuedRequestWithState> hydrateQueuedRequestWithState(QueuedRequestId queuedRequestId) {
    final String requestId = queuedRequestId.getRequestId();
    final Optional<InternalRequestStates> maybeState = requestManager.getRequestState(requestId);

    if (!maybeState.isPresent()) {
      LOG.warn(String.format("%s does not have a request status!", requestId));
      return Optional.absent();
    }

    final Optional<BaragonRequest> maybeRequest = requestManager.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      LOG.warn(String.format("%s does not have a request object!", requestId));
      return Optional.absent();
    }

    return Optional.of(new QueuedRequestWithState(queuedRequestId, maybeRequest.get(), maybeState.get()));
  }

  @VisibleForTesting
  static Comparator<QueuedRequestWithState> queuedRequestComparator() {
    return (requestA, requestB) -> {
      // noValidate & noReload comes first
      if ((requestA.getRequest().isNoValidate() && requestA.getRequest().isNoReload()) && (!requestB.getRequest().isNoReload() || !requestB.getRequest().isNoValidate())) {
        return -1;
      }

      if ((requestB.getRequest().isNoValidate() && requestB.getRequest().isNoReload()) && (!requestA.getRequest().isNoReload() || !requestA.getRequest().isNoValidate())) {
        return 1;
      }

      // Then noValidate *or* noReload
      if ((requestA.getRequest().isNoValidate() || requestA.getRequest().isNoReload()) && (!requestB.getRequest().isNoReload() && !requestB.getRequest().isNoValidate())) {
        return -1;
      }

      if ((requestB.getRequest().isNoValidate() || requestB.getRequest().isNoReload()) && (!requestA.getRequest().isNoReload() && !requestA.getRequest().isNoValidate())) {
        return 1;
      }

      // Then everything else, oldest first
      return Integer.compare(requestA.getQueuedRequestId().getIndex(), requestB.getQueuedRequestId().getIndex());
    };
  }
}

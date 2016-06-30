package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.AgentBatchResponseItem;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentRequestsStatus;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.AgentResponseId;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBatchItem;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.models.QueuedRequestWithState;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AgentManager {
  private static final Logger LOG = LoggerFactory.getLogger(AgentManager.class);

  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonAgentResponseDatastore agentResponseDatastore;
  private final AsyncHttpClient asyncHttpClient;
  private final String baragonAgentRequestUriFormat;
  private final String baragonAgentBatchRequestUriFormat;
  private final Integer baragonAgentMaxAttempts;
  private final Optional<String> baragonAuthKey;
  private final Long baragonAgentRequestTimeout;
  private final BaragonConfiguration configuration;
  private final ObjectMapper objectMapper;

  @Inject
  public AgentManager(BaragonLoadBalancerDatastore loadBalancerDatastore,
                      BaragonStateDatastore stateDatastore,
                      BaragonAgentResponseDatastore agentResponseDatastore,
                      BaragonConfiguration configuration,
                      ObjectMapper objectMapper,
                      @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient asyncHttpClient,
                      @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT) String baragonAgentRequestUriFormat,
                      @Named(BaragonDataModule.BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT) String baragonAgentBatchRequestUriFormat,
                      @Named(BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS) Integer baragonAgentMaxAttempts,
                      @Named(BaragonDataModule.BARAGON_AUTH_KEY) Optional<String> baragonAuthKey,
                      @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_TIMEOUT_MS) Long baragonAgentRequestTimeout) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.configuration = configuration;
    this.objectMapper = objectMapper;
    this.asyncHttpClient = asyncHttpClient;
    this.baragonAgentRequestUriFormat = baragonAgentRequestUriFormat;
    this.baragonAgentBatchRequestUriFormat = baragonAgentBatchRequestUriFormat;
    this.baragonAgentMaxAttempts = baragonAgentMaxAttempts;
    this.baragonAuthKey = baragonAuthKey;
    this.baragonAgentRequestTimeout = baragonAgentRequestTimeout;
  }

  private AsyncHttpClient.BoundRequestBuilder buildAgentRequest(String url, AgentRequestType requestType) {
    final BoundRequestBuilder builder;
    switch (requestType) {
      case APPLY:
        builder = asyncHttpClient.preparePost(url);
        break;
      case REVERT:
      case CANCEL:
        builder = asyncHttpClient.prepareDelete(url);
        break;
      default:
        throw new RuntimeException("Don't know how to send requests for " + requestType);
    }

    if (baragonAuthKey.isPresent()) {
      builder.addQueryParameter("authkey", baragonAuthKey.get());
    }

    return builder;
  }

  private AsyncHttpClient.BoundRequestBuilder buildAgentBatchRequest(String url, Set<BaragonRequestBatchItem> batch) throws JsonProcessingException {
    final BoundRequestBuilder builder = asyncHttpClient.preparePost(url);
    if (baragonAuthKey.isPresent()) {
      builder.addQueryParameter("authkey", baragonAuthKey.get());
    }
    builder.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    builder.setBody(objectMapper.writeValueAsBytes(batch));
    return builder;
  }

  public Map<QueuedRequestWithState, InternalRequestStates> sendRequests(final Set<QueuedRequestWithState> queuedRequestsWithState) {
    Map<QueuedRequestWithState, InternalRequestStates> results = new HashMap<>();

    Map<String, Set<BaragonRequestBatchItem>> requestsByGroup = new HashMap<>();

    for (QueuedRequestWithState queuedRequestWithState : queuedRequestsWithState) {
      final BaragonRequest request = queuedRequestWithState.getRequest();
      final Optional<BaragonService> maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());
      final Set<String> loadBalancerGroupsToUpdate = Sets.newHashSet(request.getLoadBalancerService().getLoadBalancerGroups());

      if (maybeOriginalService.isPresent()) {
        loadBalancerGroupsToUpdate.addAll(maybeOriginalService.get().getLoadBalancerGroups());
      }
      for (String group : loadBalancerGroupsToUpdate) {
        if (requestsByGroup.containsKey(group)) {
            requestsByGroup.get(group).add(new BaragonRequestBatchItem(request.getLoadBalancerRequestId(), request.getAction(), InternalStatesMap.getRequestType(queuedRequestWithState.getCurrentState())));
        } else {
          requestsByGroup.put(group, Sets.newHashSet(new BaragonRequestBatchItem(request.getLoadBalancerRequestId(), request.getAction(), InternalStatesMap.getRequestType(queuedRequestWithState.getCurrentState()))));
        }
      }
      results.put(queuedRequestWithState, InternalStatesMap.getWaitingState(queuedRequestWithState.getCurrentState()));
    }

    for (Map.Entry<String, Set<BaragonRequestBatchItem>> entry : requestsByGroup.entrySet()) {
      for (final BaragonAgentMetadata agentMetadata : loadBalancerDatastore.getAgentMetadata(entry.getKey())) {
        final String baseUrl = agentMetadata.getBaseAgentUri();
        if (agentMetadata.isBatchEnabled()) {
          sendBatchRequest(baseUrl, entry.getValue());
        } else {
          for (BaragonRequestBatchItem batchItem : entry.getValue()) {
            sendIndividualRequest(baseUrl, batchItem.getRequestId(), batchItem.getRequestType());
          }
        }
      }
    }

    return results;
  }

  private void sendBatchRequest(final String baseUrl, final Set<BaragonRequestBatchItem> originalBatch) {
    final Set<BaragonRequestBatchItem> batch = Sets.newHashSet(originalBatch);
    Set<BaragonRequestBatchItem> doNotSend = Sets.newHashSet();
    for (BaragonRequestBatchItem item : batch) {
      if (!shouldSendRequest(baseUrl, item.getRequestId(), item.getRequestType())) {
        doNotSend.add(item);
      } else {
        agentResponseDatastore.setPendingRequestStatus(item.getRequestId(), baseUrl, true);
      }
    }
    batch.removeAll(doNotSend);

    final String url = String.format(baragonAgentBatchRequestUriFormat, baseUrl);
    final Set<String> handledRequestIds = Sets.newHashSet();

    try {
      buildAgentBatchRequest(url, batch).execute(new AsyncCompletionHandler<Void>() {
        @Override
        public Void onCompleted(Response response) throws Exception {
          LOG.info("Got HTTP {} from {} for batch request {}", response.getStatusCode(), baseUrl, batch);
          if (response.getStatusCode() >= 300) {
            LOG.error("Received invalid response from agent (status: {}, response: {})", response.getStatusCode(), response.getResponseBody());
            for (BaragonRequestBatchItem item : batch) {
              agentResponseDatastore.addAgentResponse(item.getRequestId(), item.getRequestType(), baseUrl, url, Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(String.format("Caught exception processing agent response %s", response)));
              agentResponseDatastore.setPendingRequestStatus(item.getRequestId(), baseUrl, false);
              handledRequestIds.add(item.getRequestId());
            }
            return null;
          }
          Set<AgentBatchResponseItem> responses = objectMapper.readValue(response.getResponseBody(), new TypeReference<Set<AgentBatchResponseItem>>(){});
          for (AgentBatchResponseItem agentResponse : responses) {
            agentResponseDatastore.addAgentResponse(agentResponse.getRequestId(), agentResponse.getRequestType(), baseUrl, url, Optional.of(agentResponse.getStatusCode()), agentResponse.getMessage(), Optional.<String>absent());
            agentResponseDatastore.setPendingRequestStatus(agentResponse.getRequestId(), baseUrl, false);
            handledRequestIds.add(agentResponse.getRequestId());
          }
          for (BaragonRequestBatchItem item : batch) {
            if (!handledRequestIds.contains(item.getRequestId())) {
              agentResponseDatastore.addAgentResponse(item.getRequestId(), item.getRequestType(), baseUrl, url, Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(String.format("No response in batch for request %s", item.getRequestId())));
              agentResponseDatastore.setPendingRequestStatus(item.getRequestId(), baseUrl, false);
            }
          }
          return null;
        }

        @Override
        public void onThrowable(Throwable t) {
          LOG.error("Got exception when hitting {} with batch request {}", baseUrl, batch, t);
          for (BaragonRequestBatchItem item : batch) {
            if (!handledRequestIds.contains(item.getRequestId())) {
              agentResponseDatastore.addAgentResponse(item.getRequestId(), item.getRequestType(), baseUrl, url, Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(t.getMessage()));
              agentResponseDatastore.setPendingRequestStatus(item.getRequestId(), baseUrl, false);
            }
          }
        }
      });
    } catch (Exception e) {
      LOG.info("Got exception {} when hitting {} with batch reqeust {}", e, baseUrl, batch);
      for (BaragonRequestBatchItem item : batch) {
        if (!handledRequestIds.contains(item.getRequestId())) {
          agentResponseDatastore.addAgentResponse(item.getRequestId(), item.getRequestType(), baseUrl, url, Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(e.getMessage()));
          agentResponseDatastore.setPendingRequestStatus(item.getRequestId(), baseUrl, false);
        }
      }
    }
  }

  private boolean shouldSendRequest(String baseUrl, String requestId, AgentRequestType requestType) {
    Optional<Long> maybePendingRequest = agentResponseDatastore.getPendingRequest(requestId, baseUrl);
    if (maybePendingRequest.isPresent() && !((System.currentTimeMillis() - maybePendingRequest.get()) > baragonAgentRequestTimeout)) {
      LOG.info(String.format("Request has been processing for %s ms", (System.currentTimeMillis() - maybePendingRequest.get())));
      return false;
    }

    final Optional<AgentResponseId> maybeLastResponseId = agentResponseDatastore.getLastAgentResponseId(requestId, requestType, baseUrl);

    // don't retry request if we've hit the max attempts, or the request was successful
    if (maybeLastResponseId.isPresent() && (maybeLastResponseId.get().getAttempt() > baragonAgentMaxAttempts || maybeLastResponseId.get().isSuccess())) {
      return false;
    }

    return true;
  }

  private void sendIndividualRequest(final String baseUrl, final String requestId, final AgentRequestType requestType) {
    if (!shouldSendRequest(baseUrl, requestId, requestType)) {
      return;
    }

    agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, true);

    final String url = String.format(baragonAgentRequestUriFormat, baseUrl, requestId);

    try {
      buildAgentRequest(url, requestType).execute(new AsyncCompletionHandler<Void>() {
        @Override
        public Void onCompleted(Response response) throws Exception {
          LOG.info(String.format("Got HTTP %d from %s for %s", response.getStatusCode(), baseUrl, requestId));
          final Optional<String> content = Strings.isNullOrEmpty(response.getResponseBody()) ? Optional.<String>absent() : Optional.of(response.getResponseBody());
          agentResponseDatastore.addAgentResponse(requestId, requestType, baseUrl, url, Optional.of(response.getStatusCode()), content, Optional.<String>absent());
          agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, false);
          return null;
        }

        @Override
        public void onThrowable(Throwable t) {
          LOG.info(String.format("Got exception %s when hitting %s for %s", t, baseUrl, requestId));
          agentResponseDatastore.addAgentResponse(requestId, requestType, baseUrl, url, Optional.<Integer>absent(), Optional.<String>absent(), Optional.of(t.getMessage()));
          agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, false);
        }
      });
    } catch (Exception e) {
      LOG.info(String.format("Got exception %s when hitting %s for %s", e, baseUrl, requestId));
      agentResponseDatastore.addAgentResponse(requestId, requestType, baseUrl, url, Optional.<Integer>absent(), Optional.<String>absent(), Optional.of(e.getMessage()));
      agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, false);
    }
  }

  public AgentRequestsStatus getRequestsStatus(BaragonRequest request, AgentRequestType requestType) {
    boolean success = true;
    RequestAction action = request.getAction().or(RequestAction.UPDATE);
    List<Boolean> missingTemplateExceptions = new ArrayList<>();

    for (BaragonAgentMetadata agentMetadata : getAgents(request.getLoadBalancerService().getLoadBalancerGroups())) {
      final String baseUrl = agentMetadata.getBaseAgentUri();

      Optional<Long> maybePendingRequestTime = agentResponseDatastore.getPendingRequest(request.getLoadBalancerRequestId(), baseUrl);
      if (maybePendingRequestTime.isPresent()) {
        if ((System.currentTimeMillis() - maybePendingRequestTime.get()) > baragonAgentRequestTimeout) {
          LOG.info("Request {} reached maximum pending request time", request.getLoadBalancerRequestId());
          agentResponseDatastore.setPendingRequestStatus(request.getLoadBalancerRequestId(), baseUrl, false);
          return AgentRequestsStatus.FAILURE;
        } else {
          return AgentRequestsStatus.WAITING;
        }
      }

      final Optional<AgentResponseId> maybeAgentResponseId = agentResponseDatastore.getLastAgentResponseId(request.getLoadBalancerRequestId(), requestType, baseUrl);

      if (!maybeAgentResponseId.isPresent()) {
        return AgentRequestsStatus.RETRY;
      }

      Optional<AgentResponse> maybeLastResponse = agentResponseDatastore.getAgentResponse(request.getLoadBalancerRequestId(), requestType, maybeAgentResponseId.get(), baseUrl);
      boolean missingTemplate = hasMissingTemplate(maybeLastResponse);
      missingTemplateExceptions.add(missingTemplate);

      if (!missingTemplate) {
        final AgentResponseId agentResponseId = maybeAgentResponseId.get();

        if ((agentResponseId.getAttempt() < baragonAgentMaxAttempts - 1) && !agentResponseId.isSuccess()) {
          return AgentRequestsStatus.RETRY;
        } else {
          success = success && agentResponseId.isSuccess();
        }
      }
    }

    if (!missingTemplateExceptions.isEmpty() && allTrue(missingTemplateExceptions)) {
      return AgentRequestsStatus.INVALID_REQUEST_NOOP;
    } else if (success) {
      return AgentRequestsStatus.SUCCESS;
    } else {
      return action.equals(RequestAction.RELOAD) ? AgentRequestsStatus.INVALID_REQUEST_NOOP : AgentRequestsStatus.FAILURE;
    }
  }

  public Map<String, Collection<AgentResponse>> getAgentResponses(String requestId) {
    return agentResponseDatastore.getLastResponses(requestId);
  }

  public Collection<BaragonAgentMetadata> getAgents(Set<String> loadBalancerGroups) {
    return loadBalancerDatastore.getAgentMetadata(loadBalancerGroups);
  }

  public boolean invalidAgentCount(String loadBalancerGroup) {
    int agentCount = loadBalancerDatastore.getAgentMetadata(loadBalancerGroup).size();
    int targetCount = loadBalancerDatastore.getTargetCount(loadBalancerGroup).or(configuration.getDefaultTargetAgentCount());
    return (agentCount == 0 || (configuration.isEnforceTargetAgentCount() && agentCount < targetCount));
  }

  public boolean hasMissingTemplate(Optional<AgentResponse> maybeLastResponse) {
    return maybeLastResponse.isPresent() && maybeLastResponse.get().getContent().isPresent() && maybeLastResponse.get().getContent().get().contains("MissingTemplateException");
  }

  public boolean allTrue(List<Boolean> array) {
    for (boolean b : array) {
      if (!b) {
        return false;
      }
    }
    return true;
  }

  public Set<String> getAllDomainsForGroup(String group) {
    Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(group);
    Set<String> domains = new HashSet<>();
    if (maybeGroup.isPresent()) {
      domains.addAll(maybeGroup.get().getDomains());
      if (maybeGroup.get().getDefaultDomain().isPresent()) {
        domains.add(maybeGroup.get().getDefaultDomain().get());
      }
    }
    return domains;
  }
}

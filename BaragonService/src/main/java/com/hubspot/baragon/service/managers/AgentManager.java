package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentRequestsStatus;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.AgentResponseId;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.BaragonServiceModule;
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
  private final Integer baragonAgentMaxAttempts;
  private final Optional<String> baragonAuthKey;
  private final Long baragonAgentRequestTimeout;

  @Inject
  public AgentManager(BaragonLoadBalancerDatastore loadBalancerDatastore,
                      BaragonStateDatastore stateDatastore,
                      BaragonAgentResponseDatastore agentResponseDatastore,
                      @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient asyncHttpClient,
                      @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT) String baragonAgentRequestUriFormat,
                      @Named(BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS) Integer baragonAgentMaxAttempts,
                      @Named(BaragonDataModule.BARAGON_AUTH_KEY) Optional<String> baragonAuthKey,
                      @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_TIMEOUT_MS) Long baragonAgentRequestTimeout) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.asyncHttpClient = asyncHttpClient;
    this.baragonAgentRequestUriFormat = baragonAgentRequestUriFormat;
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

  public void sendRequests(final BaragonRequest request, final AgentRequestType requestType) {
    final Optional<BaragonService> maybeOriginalService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

    final Set<String> loadBalancerGroupsToUpdate = Sets.newHashSet(request.getLoadBalancerService().getLoadBalancerGroups());

    if (maybeOriginalService.isPresent()) {
      loadBalancerGroupsToUpdate.addAll(maybeOriginalService.get().getLoadBalancerGroups());
    }

    final String requestId = request.getLoadBalancerRequestId();

    for (final BaragonAgentMetadata agentMetadata : getAgents(loadBalancerGroupsToUpdate)) {
      final String baseUrl = agentMetadata.getBaseAgentUri();

      // wait until pending request has completed.
      Optional<Long> maybePendingRequest = agentResponseDatastore.getPendingRequest(requestId, baseUrl);
      if (maybePendingRequest.isPresent() && !((System.currentTimeMillis() - maybePendingRequest.get()) > baragonAgentRequestTimeout)) {
        LOG.info(String.format("Request has been processing for %s ms", (System.currentTimeMillis() - maybePendingRequest.get())));
        continue;
      }

      final Optional<AgentResponseId> maybeLastResponseId = agentResponseDatastore.getLastAgentResponseId(requestId, requestType, baseUrl);

      // don't retry request if we've hit the max attempts, or the request was successful
      if (maybeLastResponseId.isPresent() && (maybeLastResponseId.get().getAttempt() > baragonAgentMaxAttempts || maybeLastResponseId.get().isSuccess())) {
        continue;
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
          LOG.info(String.format("Request %s reached maximum pending request time", request.getLoadBalancerRequestId()));
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

  public boolean hasNoAgents(String loadBalancerGroup) {
    return loadBalancerDatastore.getAgentMetadata(loadBalancerGroup).isEmpty();
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
}

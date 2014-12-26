package com.hubspot.baragon.managers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

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

  @Inject
  public AgentManager(BaragonLoadBalancerDatastore loadBalancerDatastore,
                      BaragonStateDatastore stateDatastore,
                      BaragonAgentResponseDatastore agentResponseDatastore,
                      @Named(BaragonDataModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient asyncHttpClient,
                      @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT) String baragonAgentRequestUriFormat,
                      @Named(BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS) Integer baragonAgentMaxAttempts,
                      @Named(BaragonDataModule.BARAGON_AUTH_KEY) Optional<String> baragonAuthKey) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.asyncHttpClient = asyncHttpClient;
    this.baragonAgentRequestUriFormat = baragonAgentRequestUriFormat;
    this.baragonAgentMaxAttempts = baragonAgentMaxAttempts;
    this.baragonAuthKey = baragonAuthKey;
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

    for (final BaragonAgentMetadata agentMetadata : loadBalancerDatastore.getAgentMetadata(loadBalancerGroupsToUpdate)) {
      final String baseUrl = agentMetadata.getBaseAgentUri();

      // wait until pending request has completed.
      if (agentResponseDatastore.hasPendingRequest(requestId, baseUrl)) {
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
    final Collection<BaragonAgentMetadata> agentMetadatas = loadBalancerDatastore.getAgentMetadata(request.getLoadBalancerService().getLoadBalancerGroups());

    boolean success = true;

    for (BaragonAgentMetadata agentMetadata : agentMetadatas) {
      final String baseUrl = agentMetadata.getBaseAgentUri();
      final Optional<AgentResponseId> maybeAgentResponseId = agentResponseDatastore.getLastAgentResponseId(request.getLoadBalancerRequestId(), requestType, baseUrl);

      if (!maybeAgentResponseId.isPresent() || agentResponseDatastore.hasPendingRequest(request.getLoadBalancerRequestId(), baseUrl)) {
        return AgentRequestsStatus.WAITING;
      }

      final AgentResponseId agentResponseId = maybeAgentResponseId.get();

      if ((agentResponseId.getAttempt() < baragonAgentMaxAttempts - 1) && !agentResponseId.isSuccess()) {
        return AgentRequestsStatus.RETRY;
      } else {
        success = success && agentResponseId.isSuccess();
      }
    }

    return success ? AgentRequestsStatus.SUCCESS : AgentRequestsStatus.FAILURE;
  }

  public Map<String, Collection<AgentResponse>> getAgentResponses(String requestId) {
    return agentResponseDatastore.getLastResponses(requestId);
  }
}

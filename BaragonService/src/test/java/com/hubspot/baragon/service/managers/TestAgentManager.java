package com.hubspot.baragon.service.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.BaragonRequestBatchItem;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.ning.http.client.AsyncHttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAgentManager extends AgentManager {
  private final Map<String, String> recentRequests;
  private final Map<String, List<BaragonRequestBatchItem>> recentBatches;

  @Inject
  public TestAgentManager(
    BaragonLoadBalancerDatastore loadBalancerDatastore,
    BaragonStateDatastore stateDatastore,
    BaragonAgentResponseDatastore agentResponseDatastore,
    BaragonConfiguration configuration,
    ObjectMapper objectMapper,
    @Named(
      BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT
    ) AsyncHttpClient asyncHttpClient,
    @Named(
      BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT
    ) String baragonAgentRequestUriFormat,
    @Named(
      BaragonDataModule.BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT
    ) String baragonAgentBatchRequestUriFormat,
    @Named(BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS) Integer baragonAgentMaxAttempts,
    @Named(BaragonDataModule.BARAGON_AUTH_KEY) Optional<String> baragonAuthKey,
    @Named(
      BaragonDataModule.BARAGON_AGENT_REQUEST_TIMEOUT_MS
    ) Long baragonAgentRequestTimeout
  ) {
    super(
      loadBalancerDatastore,
      stateDatastore,
      agentResponseDatastore,
      configuration,
      objectMapper,
      asyncHttpClient,
      baragonAgentRequestUriFormat,
      baragonAgentBatchRequestUriFormat,
      baragonAgentMaxAttempts,
      baragonAuthKey,
      baragonAgentRequestTimeout
    );
    this.recentRequests = new HashMap<>();
    this.recentBatches = new HashMap<>();
  }

  @Override
  void sendIndividualRequest(
    final String baseUrl,
    final String requestId,
    final AgentRequestType requestType
  ) {
    recentRequests.put(baseUrl, requestId);
  }

  @Override
  void sendFilteredBatchRequests(
    final String baseUrl,
    final List<BaragonRequestBatchItem> batch
  ) {
    recentBatches.put(baseUrl, batch);
  }

  public void completeRequestWithFailure(
    String baseUrl,
    String url,
    String requestId,
    AgentRequestType requestType
  ) {
    agentResponseDatastore.addAgentResponse(
      requestId,
      requestType,
      baseUrl,
      url,
      Optional.absent(),
      Optional.absent(),
      Optional.of("Caught exception processing agent response")
    );
    agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, false);
  }

  public void completeRequest(
    String baseUrl,
    String url,
    String requestId,
    AgentRequestType requestType
  ) {
    agentResponseDatastore.addAgentResponse(
      requestId,
      requestType,
      baseUrl,
      url,
      Optional.of(200),
      Optional.of(""),
      Optional.<String>absent()
    );
    agentResponseDatastore.setPendingRequestStatus(requestId, baseUrl, false);
  }

  public Map<String, String> getRecentRequests() {
    return recentRequests;
  }

  public Map<String, List<BaragonRequestBatchItem>> getRecentBatches() {
    return recentBatches;
  }
}

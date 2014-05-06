package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.AgentResponseId;
import org.apache.curator.framework.CuratorFramework;

import java.util.Collections;
import java.util.List;

@Singleton
public class BaragonAgentResponseDatastore extends AbstractDataStore {
  public static final String PENDING_REQUEST_FORMAT = "/request/%s/pendingRequests/%s";

  public static final String AGENT_RESPONSES_FORMAT = "/request/%s/agent/%s-%s";
  public static final String CREATE_AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s-%s-";
  public static final String AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s";

  @Inject
  public BaragonAgentResponseDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public void addAgentResponse(String requestId, AgentRequestType requestType, String baseUrl, AgentResponse agentResponse) {
    createPersistentSequentialNode(String.format(CREATE_AGENT_RESPONSE_FORMAT, requestId, requestType, encodeUrl(baseUrl), agentResponse.getStatusCode().or(0), agentResponse.getException().isPresent()), new AgentResponse(agentResponse.getStatusCode(), agentResponse.getContent(), agentResponse.getException()));
  }

  public List<String> getAgentResponseIds(String requestId, AgentRequestType requestType, String baseUrl) {
    return getChildren(String.format(AGENT_RESPONSES_FORMAT, requestId, requestType, encodeUrl(baseUrl)));
  }

  public void setPendingRequestStatus(String requestId, String baseUrl, boolean value) {
    if (value) {
      createNode(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)));
    } else {
      deleteNode(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)));
    }
  }

  public boolean hasPendingRequest(String requestId, String baseUrl) {
    return nodeExists(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)));
  }

  public Optional<AgentResponseId> getLastAgentResponseId(String requestId, AgentRequestType requestType, String baseUrl) {
    final List<String> agentResponseIds = getAgentResponseIds(requestId, requestType, baseUrl);

    if (agentResponseIds.isEmpty()) {
      return Optional.absent();
    }

    Collections.sort(agentResponseIds, SEQUENCE_NODE_COMPARATOR_HIGH_TO_LOW);

    return Optional.of(AgentResponseId.fromString(agentResponseIds.get(0)));
  }

  public Optional<AgentResponse> getAgentResponse(String requestId, AgentRequestType requestType, String baseUrl, String responseId) {
    return readFromZk(String.format(AGENT_RESPONSE_FORMAT, requestId, requestType, encodeUrl(baseUrl), responseId), AgentResponse.class);
  }
}

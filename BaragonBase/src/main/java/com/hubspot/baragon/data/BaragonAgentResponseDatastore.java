package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.AgentRequestId;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.AgentResponseId;
import org.apache.curator.framework.CuratorFramework;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
public class BaragonAgentResponseDatastore extends AbstractDataStore {
  public static final String PENDING_REQUEST_FORMAT = "/request/%s/pendingRequests/%s";

  public static final String AGENT_REQUESTS_FORMAT = "/request/%s/agent";
  public static final String AGENT_RESPONSES_FORMAT = AGENT_REQUESTS_FORMAT + "/%s-%s";
  public static final String CREATE_AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s-%s-";
  public static final String AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s";

  @Inject
  public BaragonAgentResponseDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public AgentResponse addAgentResponse(String requestId, AgentRequestType requestType, String baseUrl, String url, Optional<Integer> statusCode, Optional<String> content, Optional<String> exception) {
    final String path = createPersistentSequentialNode(String.format(CREATE_AGENT_RESPONSE_FORMAT, requestId, requestType, encodeUrl(baseUrl), statusCode.or(0), exception.isPresent()));
    final int attempt = Integer.parseInt(path.substring(path.length() - 10));

    final AgentResponse agentResponse = new AgentResponse(url, attempt, statusCode, content, exception);

    writeToZk(path, agentResponse);

    return agentResponse;
  }

  public Collection<AgentRequestId> getAgentRequestIds(String requestId) {
    final Collection<String> nodes = getChildren(String.format(AGENT_REQUESTS_FORMAT, requestId));

    final Collection<AgentRequestId> agentRequestIds = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      agentRequestIds.add(AgentRequestId.fromString(node));
    }

    return agentRequestIds;
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

  public Map<AgentRequestType, Collection<AgentResponse>> getLastResponses(String requestId) {
    final Map<AgentRequestType, Collection<AgentResponse>> responses = Maps.newHashMap();

    for (AgentRequestId agentRequestId : getAgentRequestIds(requestId)) {
      final Optional<AgentResponseId> maybeAgentResponseId = getLastAgentResponseId(requestId, agentRequestId.getType(), agentRequestId.getBaseUrl());
      if (maybeAgentResponseId.isPresent()) {
        final Optional<AgentResponse> maybeAgentResponse = getAgentResponse(requestId, agentRequestId, maybeAgentResponseId.get());
        if (maybeAgentResponse.isPresent()) {
          if (!responses.containsKey(agentRequestId.getType())) {
            responses.put(agentRequestId.getType(), Lists.<AgentResponse>newArrayList());
          }
          responses.get(agentRequestId.getType()).add(maybeAgentResponse.get());
        }
      }
    }

    return responses;
  }

  public Optional<AgentResponse> getAgentResponse(String requestId, AgentRequestId agentRequestId, AgentResponseId agentResponseId) {
    return readFromZk(String.format(AGENT_RESPONSE_FORMAT, requestId, agentRequestId.getType(), encodeUrl(agentRequestId.getBaseUrl()), agentResponseId.getId()), AgentResponse.class);
  }
}

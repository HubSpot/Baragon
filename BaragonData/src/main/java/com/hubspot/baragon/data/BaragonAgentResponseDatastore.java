package com.hubspot.baragon.data;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.AgentRequestId;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.AgentResponseId;

@Singleton
public class BaragonAgentResponseDatastore extends AbstractDataStore {
  public static final String PENDING_REQUEST_FORMAT = "/request/%s/pendingRequests/%s";

  public static final String AGENT_REQUESTS_FORMAT = "/request/%s/agent";
  public static final String AGENT_RESPONSES_FORMAT = AGENT_REQUESTS_FORMAT + "/%s-%s";
  public static final String CREATE_AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s-%s-";
  public static final String AGENT_RESPONSE_FORMAT = AGENT_RESPONSES_FORMAT + "/%s";

  @Inject
  public BaragonAgentResponseDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  @Timed
  public AgentResponse addAgentResponse(String requestId, AgentRequestType requestType, String baseUrl, String url, Optional<Integer> statusCode, Optional<String> content, Optional<String> exception) {
    final String path = createPersistentSequentialNode(String.format(CREATE_AGENT_RESPONSE_FORMAT, requestId, requestType, encodeUrl(baseUrl), statusCode.or(0), exception.isPresent()));
    final int attempt = Integer.parseInt(path.substring(path.length() - 10));

    final AgentResponse agentResponse = new AgentResponse(url, attempt, statusCode, content, exception);

    writeToZk(path, agentResponse);

    return agentResponse;
  }

  @Timed
  public Collection<AgentRequestId> getAgentRequestIds(String requestId) {
    final Collection<String> nodes = getChildren(String.format(AGENT_REQUESTS_FORMAT, requestId));

    final Collection<AgentRequestId> agentRequestIds = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      agentRequestIds.add(AgentRequestId.fromString(node));
    }

    return agentRequestIds;
  }

  @Timed
  public List<String> getAgentResponseIds(String requestId, AgentRequestType requestType, String baseUrl) {
    return getChildren(String.format(AGENT_RESPONSES_FORMAT, requestId, requestType, encodeUrl(baseUrl)));
  }

  @Timed
  public void setPendingRequestStatus(String requestId, String baseUrl, boolean value) {
    if (value) {
      writeToZk(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)), System.currentTimeMillis());
    } else {
      deleteNode(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)));
    }
  }

  @Timed
  public Optional<Long> getPendingRequest(String requestId, String baseUrl) {
    return readFromZk(String.format(PENDING_REQUEST_FORMAT, requestId, encodeUrl(baseUrl)), Long.class);
  }

  @Timed
  public Optional<AgentResponseId> getLastAgentResponseId(String requestId, AgentRequestType requestType, String baseUrl) {
    final List<String> agentResponseIds = getAgentResponseIds(requestId, requestType, baseUrl);

    if (agentResponseIds.isEmpty()) {
      return Optional.absent();
    }

    Collections.sort(agentResponseIds, SEQUENCE_NODE_COMPARATOR_HIGH_TO_LOW);

    return Optional.of(AgentResponseId.fromString(agentResponseIds.get(0)));
  }

  @Timed
  public Map<String, Collection<AgentResponse>> getLastResponses(String requestId) {
    final Map<String, Collection<AgentResponse>> responses = Maps.newHashMap();

    for (AgentRequestId agentRequestId : getAgentRequestIds(requestId)) {
      final Optional<AgentResponseId> maybeAgentResponseId = getLastAgentResponseId(requestId, agentRequestId.getType(), agentRequestId.getBaseUrl());
      if (maybeAgentResponseId.isPresent()) {
        final Optional<AgentResponse> maybeAgentResponse = getAgentResponse(requestId, agentRequestId, maybeAgentResponseId.get());
        if (maybeAgentResponse.isPresent()) {
          if (!responses.containsKey(agentRequestId.getType().name())) {
            responses.put(agentRequestId.getType().name(), Lists.<AgentResponse>newArrayList());
          }
          responses.get(agentRequestId.getType().name()).add(maybeAgentResponse.get());
        }
      }
    }

    return responses;
  }

  @Timed
  public Optional<AgentResponse> getAgentResponse(String requestId, AgentRequestType requestType, AgentResponseId agentResponseId, String baseUrl) {
    return readFromZk(String.format(AGENT_RESPONSE_FORMAT, requestId, requestType, encodeUrl(baseUrl), agentResponseId.getId()), AgentResponse.class);
  }

  @Timed
  public Optional<AgentResponse> getAgentResponse(String requestId, AgentRequestId agentRequestId, AgentResponseId agentResponseId) {
    return readFromZk(String.format(AGENT_RESPONSE_FORMAT, requestId, agentRequestId.getType(), encodeUrl(agentRequestId.getBaseUrl()), agentResponseId.getId()), AgentResponse.class);
  }
}

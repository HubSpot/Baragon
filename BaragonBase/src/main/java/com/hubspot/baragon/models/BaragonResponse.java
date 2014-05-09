package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Map;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String loadBalancerRequestId;
  private final RequestState loadBalancerState;
  private final Optional<String> message;
  private final Optional<Map<AgentRequestType, Collection<AgentResponse>>> agentResponses;

  public static BaragonResponse failure(String requestId, String message) {
    return new BaragonResponse(requestId, RequestState.FAILED, Optional.of(message), Optional.<Map<AgentRequestType, Collection<AgentResponse>>>absent());
  }

  @JsonCreator
  public BaragonResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                         @JsonProperty("loadBalancerState") RequestState loadBalancerState,
                         @JsonProperty("message") Optional<String> message,
                         @JsonProperty("agentResponses") Optional<Map<AgentRequestType, Collection<AgentResponse>>> agentResponses) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
    this.message = message;
    this.agentResponses = agentResponses;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public RequestState getLoadBalancerState() {
    return loadBalancerState;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<Map<AgentRequestType, Collection<AgentResponse>>> getAgentResponses() {
    return agentResponses;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("loadBalancerRequestId", loadBalancerRequestId)
        .add("loadBalancerState", loadBalancerState)
        .add("message", message)
        .add("agentResponses", agentResponses)
        .toString();
  }
}

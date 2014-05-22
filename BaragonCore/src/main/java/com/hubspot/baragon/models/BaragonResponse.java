package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Map;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String loadBalancerRequestId;
  private final BaragonRequestState loadBalancerState;
  private final Optional<String> message;
  private final Optional<Map<AgentRequestType, Collection<AgentResponse>>> agentResponses;

  public static BaragonResponse failure(String requestId, String message) {
    return new BaragonResponse(requestId, BaragonRequestState.FAILED, Optional.of(message), Optional.<Map<AgentRequestType, Collection<AgentResponse>>>absent());
  }

  @JsonCreator
  public BaragonResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                         @JsonProperty("loadBalancerState") BaragonRequestState loadBalancerState,
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

  public BaragonRequestState getLoadBalancerState() {
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
    return "BaragonResponse [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerState=" + loadBalancerState +
        ", message=" + message +
        ", agentResponses=" + agentResponses +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonResponse that = (BaragonResponse) o;

    if (!agentResponses.equals(that.agentResponses)) return false;
    if (!loadBalancerRequestId.equals(that.loadBalancerRequestId)) return false;
    if (loadBalancerState != that.loadBalancerState) return false;
    if (!message.equals(that.message)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = loadBalancerRequestId.hashCode();
    result = 31 * result + loadBalancerState.hashCode();
    result = 31 * result + message.hashCode();
    result = 31 * result + agentResponses.hashCode();
    return result;
  }
}

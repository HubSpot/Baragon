package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String loadBalancerRequestId;
  private final BaragonRequestState loadBalancerState;
  private final Optional<String> message;
  private final Optional<Map<String, Collection<AgentResponse>>> agentResponses;
  private final Optional<BaragonRequest> request;

  public static BaragonResponse failure(String requestId, String message) {
    return new BaragonResponse(requestId, BaragonRequestState.FAILED, Optional.fromNullable(message), Optional.<Map<String, Collection<AgentResponse>>>absent(), Optional.<BaragonRequest>absent());
  }

  public static BaragonResponse requestDoesNotExist(String requestId) {
    return new BaragonResponse(requestId, BaragonRequestState.CANCELED, Optional.<String>of(String.format("Request %s does not exist", requestId)), Optional.<Map<String, Collection<AgentResponse>>>absent(), Optional.<BaragonRequest>absent());
  }

  public static BaragonResponse serviceNotFound(String requestId, String serviceId) {
    return new BaragonResponse(requestId, BaragonRequestState.INVALID_REQUEST_NOOP, Optional.<String>of(String.format("Service %s not found", serviceId)), Optional.<Map<String, Collection<AgentResponse>>>absent(), Optional.<BaragonRequest>absent());
  }

  @JsonCreator
  public BaragonResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                         @JsonProperty("loadBalancerState") BaragonRequestState loadBalancerState,
                         @JsonProperty("message") Optional<String> message,
                         @JsonProperty("agentResponses") Optional<Map<String, Collection<AgentResponse>>> agentResponses,
                         @JsonProperty("request") Optional<BaragonRequest> request) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
    this.message = message;
    this.agentResponses = agentResponses;
    this.request = request;
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

  public Optional<Map<String, Collection<AgentResponse>>> getAgentResponses() {
    return agentResponses;
  }

  public Optional<BaragonRequest> getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "BaragonResponse{" +
      "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
      ", loadBalancerState=" + loadBalancerState +
      ", message=" + message +
      ", agentResponses=" + agentResponses +
      ", request=" + request +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonResponse that = (BaragonResponse) o;

    if (!agentResponses.equals(that.agentResponses)) {
      return false;
    }
    if (!loadBalancerRequestId.equals(that.loadBalancerRequestId)) {
      return false;
    }
    if (loadBalancerState != that.loadBalancerState) {
      return false;
    }
    if (!message.equals(that.message)) {
      return false;
    }
    if (!request.equals(that.request)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = loadBalancerRequestId.hashCode();
    result = 31 * result + loadBalancerState.hashCode();
    result = 31 * result + message.hashCode();
    result = 31 * result + agentResponses.hashCode();
    result = 31 * result + request.hashCode();
    return result;
  }
}

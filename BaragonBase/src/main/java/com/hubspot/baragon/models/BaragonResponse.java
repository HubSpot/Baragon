package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String loadBalancerRequestId;
  private final RequestState loadBalancerState;
  private final Optional<String> message;

  @JsonCreator
  public BaragonResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                         @JsonProperty("loadBalancerState") RequestState loadBalancerState,
                         @JsonProperty("message") Optional<String> message) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
    this.message = message;
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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("loadBalancerRequestId", loadBalancerRequestId)
        .add("loadBalancerState", loadBalancerState)
        .add("message", message)
        .toString();
  }
}

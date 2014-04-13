package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String loadBalancerRequestId;
  private final RequestState loadBalancerState;

  @JsonCreator
  public BaragonResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("loadBalancerState") RequestState loadBalancerState) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerState = loadBalancerState;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public RequestState getLoadBalancerState() {
    return loadBalancerState;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("loadBalancerRequestId", loadBalancerRequestId)
        .add("loadBalancerState", loadBalancerState)
        .toString();
  }
}

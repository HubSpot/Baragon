package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonResponse {
  private final String requestId;
  private final RequestState state;

  @JsonCreator
  public BaragonResponse(@JsonProperty("requestId") String requestId, @JsonProperty("state") RequestState state) {
    this.requestId = requestId;
    this.state = state;
  }

  public String getRequestId() {
    return requestId;
  }

  public RequestState getState() {
    return state;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("requestId", requestId)
        .add("state", state)
        .toString();
  }
}

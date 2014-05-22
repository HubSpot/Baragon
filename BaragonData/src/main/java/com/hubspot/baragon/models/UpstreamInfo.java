package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class UpstreamInfo {
  private final String upstream;
  private final String requestId;

  @JsonCreator
  public UpstreamInfo(@JsonProperty("upstream") String upstream, @JsonProperty("requestId") String requestId) {
    this.upstream = upstream;
    this.requestId = requestId;
  }

  public String getUpstream() {
    return upstream;
  }

  public String getRequestId() {
    return requestId;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("upstream", upstream)
        .add("requestId", requestId)
        .toString();
  }
}

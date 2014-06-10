package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    return upstream;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UpstreamInfo that = (UpstreamInfo) o;

    if (!requestId.equals(that.requestId)) return false;
    if (!upstream.equals(that.upstream)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = upstream.hashCode();
    result = 31 * result + requestId.hashCode();
    return result;
  }
}

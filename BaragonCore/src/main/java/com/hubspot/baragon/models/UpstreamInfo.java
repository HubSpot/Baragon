package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class UpstreamInfo {
  private final String upstream;
  private final Optional<String> requestId;

  @JsonCreator
  public static UpstreamInfo fromString(String value) {
    return new UpstreamInfo(value, Optional.<String>absent());
  }

  @JsonCreator
  public UpstreamInfo(@JsonProperty("upstream") String upstream, @JsonProperty("requestId") Optional<String> requestId) {
    this.upstream = upstream;
    this.requestId = requestId;
  }

  public String getUpstream() {
    return upstream;
  }

  public Optional<String> getRequestId() {
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

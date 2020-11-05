package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicServiceContext {
  private final BaragonService service;
  private final Collection<UpstreamInfo> upstreams;
  private final Collection<BaragonConfigFile> renderedConfigs;
  private final Long timestamp;

  @JsonCreator
  public BasicServiceContext(@JsonProperty("service") BaragonService service,
                             @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams,
                             @JsonProperty("renderedConfigs") Collection<BaragonConfigFile> renderedConfigs,
                             @JsonProperty("timestamp") Long timestamp) {
    this.service = service;
    this.upstreams = MoreObjects.firstNonNull(upstreams, Collections.emptyList());
    this.renderedConfigs = renderedConfigs;
    this.timestamp = timestamp;
  }

  public BasicServiceContext(@JsonProperty("service") BaragonService service,
                             @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams,
                             @JsonProperty("renderedConfigs") Collection<BaragonConfigFile> renderedConfigs) {
    this(service, upstreams, renderedConfigs, System.currentTimeMillis());
  }

  public BasicServiceContext(@JsonProperty("service") BaragonService service,
                             @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams) {
    this(service, upstreams, Collections.EMPTY_LIST, System.currentTimeMillis());
  }

  public BaragonService getService() {
    return service;
  }

  public Collection<UpstreamInfo> getUpstreams() {
    return upstreams;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BasicServiceContext that = (BasicServiceContext) o;

    if (!service.equals(that.service)) {
      return false;
    }
    if (upstreams.size() != that.upstreams.size()) {
      return false;
    }

    for (UpstreamInfo upstreamInfo : upstreams) {
      boolean foundMatching = false;
      for (UpstreamInfo otherUpstream : that.upstreams) {
        if (UpstreamInfo.upstreamAndGroupMatches(upstreamInfo, otherUpstream)) {
          foundMatching = true;
          break;
        }
      }
      if (!foundMatching) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = service.hashCode();
    result = 31 * result + upstreams.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BasicServiceContext{" +
        "service=" + service +
        ", upstreams=" + upstreams +
        ", timestamp=" + timestamp +
        '}';
  }
}

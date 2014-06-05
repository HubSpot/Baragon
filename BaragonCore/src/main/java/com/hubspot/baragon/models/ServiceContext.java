package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hubspot.baragon.models.UpstreamInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceContext {
  private final BaragonService service;
  private final Collection<UpstreamInfo> upstreams;
  private final Long timestamp;

  @JsonCreator
  public ServiceContext(@JsonProperty("service") BaragonService service,
                        @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams,
                        @JsonProperty("timestamp") Long timestamp) {
    this.service = service;
    this.timestamp = timestamp;
    this.upstreams = Objects.firstNonNull(upstreams, Collections.<UpstreamInfo>emptyList());
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
  public int hashCode() {
    return Objects.hashCode(service, upstreams, timestamp);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(ServiceContext.class)
        .add("service", service)
        .add("upstreams", upstreams)
        .add("timestamp", timestamp)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceContext that = (ServiceContext) o;

    if (!service.equals(that.service)) return false;
    if (!timestamp.equals(that.timestamp)) return false;
    if (!upstreams.equals(that.upstreams)) return false;

    return true;
  }
}

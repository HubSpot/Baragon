package com.hubspot.baragon.models;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonServiceState {
  private final BaragonService service;
  private final Collection<UpstreamInfo> upstreams;

  @JsonCreator
  public BaragonServiceState(@JsonProperty("service") BaragonService service,
                             @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams) {
    this.service = service;
    this.upstreams = upstreams;
  }

  public BaragonService getService() {
    return service;
  }

  public Collection<UpstreamInfo> getUpstreams() {
    return upstreams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonServiceState that = (BaragonServiceState) o;

    if (!service.equals(that.service)) {
      return false;
    }
    if (!upstreams.equals(that.upstreams)) {
      return false;
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
    return "BaragonServiceState [" +
        "service=" + service +
        ", upstreams=" + upstreams +
        ']';
  }
}

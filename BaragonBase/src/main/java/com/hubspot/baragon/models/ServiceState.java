package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.Collection;

public class ServiceState {
  private final Service service;
  private final Collection<String> upstreams;

  @JsonCreator
  public ServiceState(@JsonProperty("service") Service service, @JsonProperty("upstreams") Collection<String> upstreams) {
    this.service = service;
    this.upstreams = upstreams;
  }

  public Service getService() {
    return service;
  }

  public Collection<String> getUpstreams() {
    return upstreams;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("service", service)
        .add("upstreams", upstreams)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(service, upstreams);
  }
}

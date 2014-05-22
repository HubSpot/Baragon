package com.hubspot.baragon.service.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hubspot.baragon.models.BaragonService;

import java.util.Collection;

public class ServiceState {
  private final BaragonService service;
  private final Collection<String> upstreams;

  @JsonCreator
  public ServiceState(@JsonProperty("service") BaragonService service, @JsonProperty("upstreams") Collection<String> upstreams) {
    this.service = service;
    this.upstreams = upstreams;
  }

  public BaragonService getService() {
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

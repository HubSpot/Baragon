package com.hubspot.baragon.models;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.ServiceInfo;

public class ServiceInfoAndUpstreams {
  private final ServiceInfo serviceInfo;
  private final Collection<String> upstreams;
  
  @JsonCreator
  public ServiceInfoAndUpstreams(@JsonProperty("serviceInfo") ServiceInfo serviceInfo, @JsonProperty("upstreams") Collection<String> upstreams) {
    this.serviceInfo = serviceInfo;
    this.upstreams = upstreams;
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public Collection<String> getUpstreams() {
    return upstreams;
  }
}

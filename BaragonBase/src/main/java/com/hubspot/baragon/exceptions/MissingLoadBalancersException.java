package com.hubspot.baragon.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.ServiceInfo;

import java.util.Collection;

public class MissingLoadBalancersException extends RuntimeException {
  private final ServiceInfo serviceInfo;
  private final Collection<String> missingLoadBalancers;

  public MissingLoadBalancersException(ServiceInfo serviceInfo, Collection<String> missingLoadBalancers) {
    this.serviceInfo = serviceInfo;
    this.missingLoadBalancers = missingLoadBalancers;
  }

  public MissingLoadBalancersException(Entity e) {
    this.serviceInfo = e.getServiceInfo();
    this.missingLoadBalancers = e.getMissingLoadBalancers();
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public Collection<String> getMissingLoadBalancers() {
    return missingLoadBalancers;
  }

  public Entity getEntity() {
    return new Entity(serviceInfo, missingLoadBalancers);
  }

  public static class Entity {
    private final ServiceInfo serviceInfo;
    private final Collection<String> missingLoadBalancers;

    @JsonCreator
    public Entity(@JsonProperty("serviceInfo") ServiceInfo serviceInfo, @JsonProperty("missingLoadBalancers") Collection<String> missingLoadBalancers) {
      this.serviceInfo = serviceInfo;
      this.missingLoadBalancers = missingLoadBalancers;
    }

    public ServiceInfo getServiceInfo() {
      return serviceInfo;
    }

    public Collection<String> getMissingLoadBalancers() {
      return missingLoadBalancers;
    }
  }
}

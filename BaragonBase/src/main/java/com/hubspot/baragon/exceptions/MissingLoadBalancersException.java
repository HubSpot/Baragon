package com.hubspot.baragon.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.Service;

import java.util.Collection;

public class MissingLoadBalancersException extends RuntimeException {
  private final Service service;
  private final Collection<String> missingLoadBalancers;

  public MissingLoadBalancersException(Service service, Collection<String> missingLoadBalancers) {
    this.service = service;
    this.missingLoadBalancers = missingLoadBalancers;
  }

  public MissingLoadBalancersException(Entity e) {
    this.service = e.getService();
    this.missingLoadBalancers = e.getMissingLoadBalancers();
  }

  public Service getService() {
    return service;
  }

  public Collection<String> getMissingLoadBalancers() {
    return missingLoadBalancers;
  }

  public Entity getEntity() {
    return new Entity(service, missingLoadBalancers);
  }

  public static class Entity {
    private final Service service;
    private final Collection<String> missingLoadBalancers;

    @JsonCreator
    public Entity(@JsonProperty("service") Service service, @JsonProperty("missingLoadBalancers") Collection<String> missingLoadBalancers) {
      this.service = service;
      this.missingLoadBalancers = missingLoadBalancers;
    }

    public Service getService() {
      return service;
    }

    public Collection<String> getMissingLoadBalancers() {
      return missingLoadBalancers;
    }
  }
}

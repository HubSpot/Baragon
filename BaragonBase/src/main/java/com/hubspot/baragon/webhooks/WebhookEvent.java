package com.hubspot.baragon.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.ServiceInfo;

public class WebhookEvent {
  private final EventType eventType;
  private final String loadBalancer;
  private final ServiceInfo serviceInfo;
  private final String upstream;

  @JsonCreator
  public WebhookEvent(@JsonProperty("eventType") EventType eventType,
                      @JsonProperty("loadBalancer") String loadBalancer,
                      @JsonProperty("serviceInfo") ServiceInfo serviceInfo, @JsonProperty("upstream") String upstream) {
    this.loadBalancer = loadBalancer;
    this.serviceInfo = serviceInfo;
    this.upstream = upstream;
    this.eventType = eventType;
  }

  public EventType getEventType() {
    return eventType;
  }

  public String getLoadBalancer() {
    return loadBalancer;
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public String getUpstream() {
    return upstream;
  }

  public static enum EventType {
    SERVICE_ADDED,
    SERVICE_ACTIVE,
    SERVICE_REMOVED,
    UPSTREAM_ADDED,
    UPSTREAM_HEALTHY,
    UPSTREAM_UNHEALTHY,
    UPSTREAM_REMOVED,
  }
}

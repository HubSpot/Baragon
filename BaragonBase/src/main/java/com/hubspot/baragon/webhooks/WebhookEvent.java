package com.hubspot.baragon.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.ServiceInfo;

public class WebhookEvent {
  private final ServiceInfo serviceInfo;
  private final String upstream;
  private final Boolean healthy;

  @JsonCreator
  public WebhookEvent(@JsonProperty("serviceInfo") ServiceInfo serviceInfo, @JsonProperty("upstream") String upstream,
                      @JsonProperty("healthy") Boolean healthy) {
    this.serviceInfo = serviceInfo;
    this.upstream = upstream;
    this.healthy = healthy;
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public String getUpstream() {
    return upstream;
  }

  public Boolean getHealthy() {
    return healthy;
  }
}

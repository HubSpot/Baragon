package com.hubspot.baragon.service.config;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.service.edgecache.cloudflare.EdgeCacheClass;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeCacheConfiguration {

  @JsonProperty
  @NotNull
  private boolean enabled = false;

  @JsonProperty
  @NotNull
  private EdgeCacheClass edgeCache = EdgeCacheClass.CLOUDFLARE;

  @JsonProperty
  @NotNull
  private Map<String, String> integrationSettings = new HashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public EdgeCacheConfiguration setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public EdgeCacheClass getEdgeCache() {
    return edgeCache;
  }

  public EdgeCacheConfiguration setEdgeCache(EdgeCacheClass edgeCache) {
    this.edgeCache = edgeCache;
    return this;
  }

  public Map<String, String> getIntegrationSettings() {
    return integrationSettings;
  }

  public EdgeCacheConfiguration setIntegrationSettings(Map<String, String> integrationSettings) {
    this.integrationSettings = integrationSettings;
    return this;
  }

}

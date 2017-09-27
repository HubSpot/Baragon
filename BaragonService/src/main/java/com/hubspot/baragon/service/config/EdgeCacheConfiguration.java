package com.hubspot.baragon.service.config;

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

  // The following three values are used by the Cloudflare EdgeCache implementation.
  // TODO: It's kind of a smell to have implementation-specific configs in the generic EdgeCacheConfiguration block.
  // TODO: Is there a better way of doing this?
  @JsonProperty
  @NotNull
  private String apiEmail;

  @JsonProperty
  @NotNull
  private String apiKey;

  @JsonProperty
  @NotNull
  private String apiBase;

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

  public String getApiEmail() {
    return apiEmail;
  }

  public EdgeCacheConfiguration setApiEmail(String apiEmail) {
    this.apiEmail = apiEmail;
    return this;
  }

  public String getApiKey() {
    return apiKey;
  }

  public EdgeCacheConfiguration setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public String getApiBase() {
    return apiBase;
  }

  public EdgeCacheConfiguration setApiBase(String apiBase) {
    this.apiBase = apiBase;
    return this;
  }

}

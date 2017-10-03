package com.hubspot.baragon.service.edgecache.cloudflare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class CloudflareDnsRecord {
  private final Boolean proxied;
  private final String zoneName;

  public CloudflareDnsRecord(@JsonProperty("proxied") Boolean proxied, @JsonProperty("zone_name") String zoneName) {
    this.proxied = proxied;
    this.zoneName = zoneName;
  }

  public Boolean isProxied() {
    return proxied;
  }

  public String getZoneName() {
    return zoneName;
  }
}

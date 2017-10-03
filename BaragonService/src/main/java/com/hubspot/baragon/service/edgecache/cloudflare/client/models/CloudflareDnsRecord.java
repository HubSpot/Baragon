package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloudflareDnsRecord that = (CloudflareDnsRecord) o;
    return Objects.equal(proxied, that.proxied) &&
        Objects.equal(zoneName, that.zoneName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(proxied, zoneName);
  }
}

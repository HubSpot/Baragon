package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class CloudflareDnsRecord {
  private final Boolean proxied;
  private final String name;

  public CloudflareDnsRecord(@JsonProperty("proxied") Boolean proxied, @JsonProperty("name") String name) {
    this.proxied = proxied;
    this.name = name;
  }

  public Boolean isProxied() {
    return proxied;
  }

  public String getName() {
    return name;
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
        Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(proxied, name);
  }

  @Override
  public String toString() {
    return "CloudflareDnsRecord{" +
        "proxied=" + proxied +
        ", name='" + name + '\'' +
        '}';
  }
}

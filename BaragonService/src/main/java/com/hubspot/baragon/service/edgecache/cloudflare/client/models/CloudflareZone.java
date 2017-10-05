package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class CloudflareZone {
  private final String id;
  private final String name;

  @JsonCreator
  public CloudflareZone(@JsonProperty("id") String id, @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
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
    CloudflareZone that = (CloudflareZone) o;
    return Objects.equal(id, that.id) &&
        Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, name);
  }

  @Override
  public String toString() {
    return "CloudflareZone{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}

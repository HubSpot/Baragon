package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonSource {
  private final String name;
  private final Optional<Integer> port;

  @JsonCreator
  public BaragonSource(@JsonProperty("name") String name,
                       @JsonProperty("port") Optional<Integer> port) {
    this.name = name;
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public Optional<Integer> getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonSource source = (BaragonSource) o;

    if (name != null ? !name.equals(source.name) : source.name != null) {
      return false;
    }
    return port != null ? port.equals(source.port) : source.port == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (port != null ? port.hashCode() : 0);
    return result;
  }
}

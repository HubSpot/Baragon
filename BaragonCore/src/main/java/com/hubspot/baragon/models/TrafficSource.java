package com.hubspot.baragon.models;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrafficSource {
  @Size(min = 1)
  private final String name;

  @NotNull
  private final TrafficSourceType type;

  @JsonCreator
  public static TrafficSource fromString(String input) {
    return new TrafficSource(input, TrafficSourceType.CLASSIC);
  }

  @JsonCreator
  public TrafficSource(@JsonProperty("name") String name, @JsonProperty("type") TrafficSourceType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public TrafficSourceType getType() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof TrafficSource) {
      final TrafficSource that = (TrafficSource) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.type, that.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  public String toString() {
    return "TrafficSource{" +
        "name='" + name + '\'' +
        ", type=" + type +
        '}';
  }
}

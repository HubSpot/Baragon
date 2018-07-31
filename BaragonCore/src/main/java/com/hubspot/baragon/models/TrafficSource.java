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

  private final RegisterBy registerBy;

  @JsonCreator
  public static TrafficSource fromString(String input) {
    return new TrafficSource(input, TrafficSourceType.CLASSIC, RegisterBy.INSTANCE_ID);
  }

  public TrafficSource(String name, TrafficSourceType type) {
    this(name, type, RegisterBy.INSTANCE_ID);
  }

  @JsonCreator
  public TrafficSource(@JsonProperty("name") String name, @JsonProperty("type") TrafficSourceType type, @JsonProperty("registerBy") RegisterBy registerBy) {
    this.name = name;
    this.type = type;
    this.registerBy = registerBy == null ? RegisterBy.INSTANCE_ID : registerBy;
  }

  public String getName() {
    return name;
  }

  public TrafficSourceType getType() {
    return type;
  }

  public RegisterBy getRegisterBy() {
    return registerBy;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof TrafficSource) {
      final TrafficSource that = (TrafficSource) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.type, that.type) &&
          Objects.equals(this.registerBy, that.registerBy);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, registerBy);
  }

  @Override
  public String toString() {
    return "TrafficSource{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", registerBy=" + registerBy +
        '}';
  }
}

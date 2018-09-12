package com.hubspot.baragon.models;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrafficSource {
  @Size(min = 1)
  private final String name;

  @NotNull
  private final TrafficSourceType type;

  private final RegisterBy registerBy;
  private final CustomPortType customPortType;

  @JsonCreator
  public static TrafficSource fromString(String input) {
    return new TrafficSource(input, TrafficSourceType.CLASSIC, RegisterBy.INSTANCE_ID, CustomPortType.NONE);
  }

  public TrafficSource(String name, TrafficSourceType type) {
    this(name, type, RegisterBy.INSTANCE_ID, CustomPortType.NONE);
  }

  @JsonCreator
  public TrafficSource(@JsonProperty("name") String name,
                       @JsonProperty("type") TrafficSourceType type,
                       @JsonProperty("registerBy") RegisterBy registerBy,
                       @JsonProperty("customPortType") CustomPortType customPortType) {
    this.name = name;
    this.type = type;
    this.registerBy = registerBy == null ? RegisterBy.INSTANCE_ID : registerBy;
    this.customPortType = customPortType == null ? CustomPortType.NONE : customPortType;
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

  public CustomPortType getCustomPortType() {
    return customPortType;
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
          Objects.equals(this.registerBy, that.registerBy) &&
          Objects.equals(this.customPortType, that.customPortType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, registerBy, customPortType);
  }

  @Override
  public String toString() {
    return "TrafficSource{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", registerBy=" + registerBy +
        ", customPortType=" + customPortType +
        '}';
  }
}

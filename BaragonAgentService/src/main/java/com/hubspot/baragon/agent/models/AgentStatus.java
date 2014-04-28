package com.hubspot.baragon.agent.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AgentStatus {
  private final String name;
  private final boolean validConfigs;

  @JsonCreator
  public AgentStatus(@JsonProperty("name") String name, @JsonProperty("validConfigs") boolean validConfigs) {
    this.name = name;
    this.validConfigs = validConfigs;
  }

  public String getName() {
    return name;
  }

  public boolean isValidConfigs() {
    return validConfigs;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(AgentStatus.class)
        .add("name", name)
        .add("validConfigs", validConfigs)
        .toString();
  }
}

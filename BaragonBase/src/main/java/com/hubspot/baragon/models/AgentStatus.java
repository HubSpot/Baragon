package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class AgentStatus {
  private final String name;
  private final boolean validConfigs;
  private final Optional<String> errorMessage;
  private final boolean leader;
  private final String mostRecentRequestId;

  @JsonCreator
  public AgentStatus(@JsonProperty("name") String name,
                     @JsonProperty("validConfigs") boolean validConfigs,
                     @JsonProperty("errorMessage") Optional<String> errorMessage,
                     @JsonProperty("leader") boolean leader,
                     @JsonProperty("mostRecentRequestId") String mostRecentRequestId) {
    this.name = name;
    this.validConfigs = validConfigs;
    this.errorMessage = errorMessage;
    this.leader = leader;
    this.mostRecentRequestId = mostRecentRequestId;
  }

  public String getName() {
    return name;
  }

  public boolean isValidConfigs() {
    return validConfigs;
  }

  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  public boolean isLeader() {
    return leader;
  }

  public String getMostRecentRequestId() {
    return mostRecentRequestId;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("validConfigs", validConfigs)
        .add("errorMessage", errorMessage)
        .add("leader", leader)
        .add("mostRecentRequestId", mostRecentRequestId)
        .toString();
  }
}

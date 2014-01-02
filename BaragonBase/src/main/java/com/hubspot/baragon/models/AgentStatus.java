package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AgentStatus {
  private final String name;
  private final boolean leader;
  private final long lastPoll;
  private final boolean validConfigs;

  @JsonCreator
  public AgentStatus(@JsonProperty("name") String name, @JsonProperty("leader") boolean leader,
                     @JsonProperty("lastPoll") long lastPoll, @JsonProperty("validConfigs") boolean validConfigs) {
    this.name = name;
    this.leader = leader;
    this.lastPoll = lastPoll;
    this.validConfigs = validConfigs;
  }

  public String getName() {
    return name;
  }

  public boolean isLeader() {
    return leader;
  }

  public long getLastPoll() {
    return lastPoll;
  }

  public boolean isValidConfigs() {
    return validConfigs;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(AgentStatus.class)
        .add("name", name)
        .add("leader", leader)
        .add("lastPoll", lastPoll)
        .add("validConfigs", validConfigs)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, leader, lastPoll, validConfigs);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof AgentStatus) {
      return Objects.equal(name, ((AgentStatus)that).getName())
          && Objects.equal(leader, ((AgentStatus)that).isLeader())
          && Objects.equal(lastPoll, ((AgentStatus)that).getLastPoll())
          && Objects.equal(validConfigs, ((AgentStatus)that).isValidConfigs());
    }

    return false;
  }
}

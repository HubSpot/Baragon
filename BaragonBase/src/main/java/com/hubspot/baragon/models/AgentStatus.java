package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}

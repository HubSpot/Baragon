package com.hubspot.baragon.models;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentStatus {
  private final String group;
  private final boolean validConfigs;
  private final Optional<String> errorMessage;
  private final Set<String> stateErrors;
  private final boolean leader;
  private final String mostRecentRequestId;
  private final String zookeeperState;
  private final BaragonAgentMetadata agentInfo;
  private final BaragonAgentState agentState;

  @JsonCreator
  public BaragonAgentStatus(@JsonProperty("group") String group,
                            @JsonProperty("validConfigs") boolean validConfigs,
                            @JsonProperty("errorMessage") Optional<String> errorMessage,
                            @JsonProperty("leader") boolean leader,
                            @JsonProperty("mostRecentRequestId") String mostRecentRequestId,
                            @JsonProperty("zookeeperState") String zookeeperState,
                            @JsonProperty("agentInfo") BaragonAgentMetadata agentInfo,
                            @JsonProperty("agentState") BaragonAgentState agentState,
                            @JsonProperty("stateErrors") Set<String> stateErrors) {
    this.group = group;
    this.validConfigs = validConfigs;
    this.errorMessage = errorMessage;
    this.leader = leader;
    this.mostRecentRequestId = mostRecentRequestId;
    this.zookeeperState = zookeeperState;
    this.agentInfo = agentInfo;
    this.agentState = agentState;
    this.stateErrors = stateErrors;
  }

  public String getGroup() {
    return group;
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

  public String getZookeeperState() {
    return zookeeperState;
  }

  public BaragonAgentMetadata getAgentInfo() {
    return agentInfo;
  }

  public BaragonAgentState getAgentState() {
    return agentState;
  }

  public Set<String> getStateErrors() {
    return stateErrors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaragonAgentStatus that = (BaragonAgentStatus) o;
    return validConfigs == that.validConfigs &&
        leader == that.leader &&
        Objects.equals(group, that.group) &&
        Objects.equals(errorMessage, that.errorMessage) &&
        Objects.equals(stateErrors, that.stateErrors) &&
        Objects.equals(mostRecentRequestId, that.mostRecentRequestId) &&
        Objects.equals(zookeeperState, that.zookeeperState) &&
        Objects.equals(agentInfo, that.agentInfo) &&
        agentState == that.agentState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, validConfigs, errorMessage, stateErrors, leader, mostRecentRequestId, zookeeperState, agentInfo, agentState);
  }

  @Override
  public String toString() {
    return "BaragonAgentStatus{" +
        "group='" + group + '\'' +
        ", validConfigs=" + validConfigs +
        ", errorMessage=" + errorMessage +
        ", stateErrors=" + stateErrors +
        ", leader=" + leader +
        ", mostRecentRequestId='" + mostRecentRequestId + '\'' +
        ", zookeeperState='" + zookeeperState + '\'' +
        ", agentInfo=" + agentInfo +
        ", agentState=" + agentState +
        '}';
  }
}

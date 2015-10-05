package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentStatus {
  private final String group;
  private final boolean validConfigs;
  private final Optional<String> errorMessage;
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
                            @JsonProperty("agentState") BaragonAgentState agentState) {
    this.group = group;
    this.validConfigs = validConfigs;
    this.errorMessage = errorMessage;
    this.leader = leader;
    this.mostRecentRequestId = mostRecentRequestId;
    this.zookeeperState = zookeeperState;
    this.agentInfo = agentInfo;
    this.agentState = agentState;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonAgentStatus that = (BaragonAgentStatus) o;

    if (leader != that.leader) {
      return false;
    }
    if (validConfigs != that.validConfigs) {
      return false;
    }
    if (!errorMessage.equals(that.errorMessage)) {
      return false;
    }
    if (!group.equals(that.group)) {
      return false;
    }
    if (mostRecentRequestId != null ? !mostRecentRequestId.equals(that.mostRecentRequestId) : that.mostRecentRequestId != null) {
      return false;
    }
    if (!zookeeperState.equals(that.zookeeperState)) {
      return false;
    }
    if (!agentInfo.equals(that.agentInfo)) {
      return false;
    }
    if (!agentState.equals(that.agentState)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = group.hashCode();
    result = 31 * result + (validConfigs ? 1 : 0);
    result = 31 * result + errorMessage.hashCode();
    result = 31 * result + (leader ? 1 : 0);
    result = 31 * result + (mostRecentRequestId != null ? mostRecentRequestId.hashCode() : 0);
    result = 31 * result + zookeeperState.hashCode();
    result = 31 * result + agentInfo.hashCode();
    result = 31 * result + agentState.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BaragonAgentStatus [" +
        "group='" + group +
        ", validConfigs=" + validConfigs +
        ", errorMessage=" + errorMessage +
        ", leader=" + leader +
        ", mostRecentRequestId='" + mostRecentRequestId +
        ", zookeeperState='" + zookeeperState +
        ", agentInfo='" + agentInfo +
        ", agentState=" + agentState +
        ']';
  }
}

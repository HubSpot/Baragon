package com.hubspot.baragon.config;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

public class AgentElbConfiguration {

  @NotNull
  @JsonProperty("instanceId")
  private String instanceId;

  @NotNull
  @JsonProperty("elbNames")
  private List<String> elbNames;

  public List<String> getElbNames() {
    return elbNames;
  }

  public void setElbNames(List<String> elbNames) {
    this.elbNames = elbNames;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }
}

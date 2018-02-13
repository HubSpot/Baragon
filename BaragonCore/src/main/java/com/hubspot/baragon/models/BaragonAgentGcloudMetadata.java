package com.hubspot.baragon.models;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class BaragonAgentGcloudMetadata {
  private final String resourceGroup;
  private final String project;
  private final Optional<String> region;
  private final String backendService;
  private final String instanceName;

  public BaragonAgentGcloudMetadata(@JsonProperty("resourceGroup") String resourceGroup,
                                    @JsonProperty("project") String project,
                                    @JsonProperty("region") Optional<String> region,
                                    @JsonProperty("backendService") String backendService,
                                    @JsonProperty("instanceName") String instanceName) {
    this.resourceGroup = resourceGroup;
    this.project = project;
    this.region = region;
    this.backendService = backendService;
    this.instanceName = instanceName;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public String getProject() {
    return project;
  }

  public Optional<String> getRegion() {
    return region;
  }

  public String getBackendService() {
    return backendService;
  }

  public String getInstanceName() {
    return instanceName;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BaragonAgentGcloudMetadata) {
      final BaragonAgentGcloudMetadata that = (BaragonAgentGcloudMetadata) obj;
      return Objects.equals(this.resourceGroup, that.resourceGroup) &&
          Objects.equals(this.project, that.project) &&
          Objects.equals(this.region, that.region) &&
          Objects.equals(this.backendService, that.backendService) &&
          Objects.equals(this.instanceName, that.instanceName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceGroup, project, region, backendService, instanceName);
  }

  @Override
  public String toString() {
    return "BaragonAgentGcloudMetadata{" +
        "resourceGroup='" + resourceGroup + '\'' +
        ", project='" + project + '\'' +
        ", region='" + region + '\'' +
        ", backendService='" + backendService + '\'' +
        ", instanceName='" + instanceName + '\'' +
        '}';
  }
}

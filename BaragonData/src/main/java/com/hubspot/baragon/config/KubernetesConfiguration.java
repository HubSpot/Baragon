package com.hubspot.baragon.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.ImmutableList;

public class KubernetesConfiguration {
  private boolean enabled = false;
  private String basePathAnnotation = "baragon.hubspot.com/basePath";
  private Map<String, String> baragonLabelFilter = Collections.emptyMap();
  private String serviceNameLabel = "baragon.hubspot.com/serviceName";
  private String upstreamGroupsLabel = "baragon.hubspot.com/upstreamGroup";

  @NotEmpty
  private List<String> upstreamGroups = ImmutableList.of("k8s");

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBasePathAnnotation() {
    return basePathAnnotation;
  }

  public void setBasePathAnnotation(String basePathAnnotation) {
    this.basePathAnnotation = basePathAnnotation;
  }

  public Map<String, String> getBaragonLabelFilter() {
    return baragonLabelFilter;
  }

  public void setBaragonLabelFilter(Map<String, String> baragonLabelFilter) {
    this.baragonLabelFilter = baragonLabelFilter;
  }

  public String getServiceNameLabel() {
    return serviceNameLabel;
  }

  public void setServiceNameLabel(String serviceNameLabel) {
    this.serviceNameLabel = serviceNameLabel;
  }

  public String getUpstreamGroupsLabel() {
    return upstreamGroupsLabel;
  }

  public void setUpstreamGroupsLabel(String upstreamGroupsLabel) {
    this.upstreamGroupsLabel = upstreamGroupsLabel;
  }

  public List<String> getUpstreamGroups() {
    return upstreamGroups;
  }

  public String[] getUpstreamGroupsAsArray() {
    return (String[]) upstreamGroups.toArray();
  }

  public void setUpstreamGroups(List<String> upstreamGroups) {
    this.upstreamGroups = upstreamGroups;
  }
}

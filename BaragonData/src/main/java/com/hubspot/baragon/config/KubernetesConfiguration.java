package com.hubspot.baragon.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.ImmutableList;

public class KubernetesConfiguration {
  private boolean enabled = false;
  private String basePathAnnotation = "baragon.hubspot.com/base-path";
  private Map<String, String> baragonLabelFilter = Collections.emptyMap();
  private String serviceNameLabel = "baragon.hubspot.com/service-name";
  private String upstreamGroupsLabel = "baragon.hubspot.com/upstream-group";
  private String lbGroupsAnnotation = "baragon.hubspot.com/lb-groups";
  private String domainsAnnotation = "baragon.hubspot.com/domains";
  private String ownersAnnotation = "baragon.hubspot.com/owners";
  private String templateNameAnnotation = "baragon.hubspot.com/template-name";

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

  public String getLbGroupsAnnotation() {
    return lbGroupsAnnotation;
  }

  public void setLbGroupsAnnotation(String lbGroupsAnnotation) {
    this.lbGroupsAnnotation = lbGroupsAnnotation;
  }

  public String getDomainsAnnotation() {
    return domainsAnnotation;
  }

  public void setDomainsAnnotation(String domainsAnnotation) {
    this.domainsAnnotation = domainsAnnotation;
  }

  public String getOwnersAnnotation() {
    return ownersAnnotation;
  }

  public void setOwnersAnnotation(String ownersAnnotation) {
    this.ownersAnnotation = ownersAnnotation;
  }

  public String getTemplateNameAnnotation() {
    return templateNameAnnotation;
  }

  public void setTemplateNameAnnotation(String templateNameAnnotation) {
    this.templateNameAnnotation = templateNameAnnotation;
  }
}

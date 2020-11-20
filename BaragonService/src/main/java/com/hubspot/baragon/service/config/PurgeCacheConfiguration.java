package com.hubspot.baragon.service.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.BaragonService;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PurgeCacheConfiguration {
  @JsonProperty("enabledTemplates")
  @NotNull
  private List<String> enabledTemplates;

  @JsonProperty(value = "excludedServiceIds")
  @NotNull
  private List<String> excludedServiceIds;

  public List<String> getEnabledTemplates() {
    if (enabledTemplates == null) {
      return new ArrayList<>();
    }
    return enabledTemplates;
  }

  public void setEnabledTemplates(List<String> enabledTemplates) {
    this.enabledTemplates = enabledTemplates;
  }

  public List<String> getExcludedServiceIds() {
    if (excludedServiceIds == null) {
      return new ArrayList<>();
    }
    return excludedServiceIds;
  }

  public void setExcludedServiceIds(List<String> excludedServiceIds) {
    this.excludedServiceIds = excludedServiceIds;
  }

  public boolean serviceShouldPurgeCache(BaragonService service){
    // 1. if the serviceId is on the exclude list, return false
    if (getExcludedServiceIds().contains(service.getServiceId())){
      return false;
    }
    // 2. if the service's templateName is not in the enabledTemplates list, return false, otherwise return true
    return getEnabledTemplates().contains(service.getTemplateName().or(""));
  }


}

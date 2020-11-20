package com.hubspot.baragon.service.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.BaragonService;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PurgeCacheConfiguration {
  @JsonProperty("enabledTemplates")
  private List<String> enabledTemplates;

  @JsonProperty("excludedServiceIds")
  private List<String> excludedServiceIds;

  public List<String> getEnabledTemplates() {
    return enabledTemplates;
  }

  public void setEnabledTemplates(List<String> enabledTemplates) {
    this.enabledTemplates = enabledTemplates;
  }

  public List<String> getExcludedServiceIds() {
    return excludedServiceIds;
  }

  public void setExcludedServiceIds(List<String> excludedServiceIds) {
    this.excludedServiceIds = excludedServiceIds;
  }

  public boolean serviceShouldPurgeCache(BaragonService service){
    // 1. if the serviceId is on the exclude list, return false
    if (excludedServiceIds.contains(service.getServiceId())){
      return false;
    }
    // 2. if the service's templateName is not in the enabledTemplates list, return false, otherwise return true
    return enabledTemplates.contains(service.getTemplateName().or(""));
  }


}

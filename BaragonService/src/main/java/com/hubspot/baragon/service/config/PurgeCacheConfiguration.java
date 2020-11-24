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


}

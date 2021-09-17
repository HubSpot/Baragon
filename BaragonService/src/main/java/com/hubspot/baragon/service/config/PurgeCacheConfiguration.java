package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.models.BaragonService;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PurgeCacheConfiguration {
  @JsonProperty("enabledTemplates")
  @NotNull
  private List<String> enabledTemplates = new ArrayList<>();

  @JsonProperty(value = "excludedServiceIds")
  @NotNull
  private List<String> excludedServiceIds = new ArrayList<>();

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
}

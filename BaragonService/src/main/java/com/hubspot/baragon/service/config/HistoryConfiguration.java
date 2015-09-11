package com.hubspot.baragon.service.config;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class HistoryConfiguration {
  @JsonProperty("enabled")
  boolean enabled = true;

  @JsonProperty("workerInitialDelayHours")
  int workerInitialDelayHours = 0;

  @JsonProperty("purgeOldRequests")
  boolean purgeOldRequests = false;

  @JsonProperty("purgeOldRequestsAfterDays")
  @Min(1)
  int purgeOldRequestsAfterDays = 30;

  @JsonProperty("purgeWhenDateNotFound")
  boolean purgeWhenDateNotFound = false;

  @JsonProperty("purgeEveryHours")
  @Min(1)
  int purgeEveryHours = 24;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getWorkerInitialDelayHours() {
    return workerInitialDelayHours;
  }

  public void setWorkerInitialDelayHours(int workerInitialDelayHours) {
    this.workerInitialDelayHours = workerInitialDelayHours;
  }

  public boolean isPurgeOldRequests() {
    return purgeOldRequests;
  }

  public void setPurgeOldRequests(boolean purgeOldRequests) {
    this.purgeOldRequests = purgeOldRequests;
  }

  public int getPurgeOldRequestsAfterDays() {
    return purgeOldRequestsAfterDays;
  }

  public boolean isPurgeWhenDateNotFound() {
    return purgeWhenDateNotFound;
  }

  public void setPurgeWhenDateNotFound(boolean purgeWhenDateNotFound) {
    this.purgeWhenDateNotFound = purgeWhenDateNotFound;
  }

  public void setPurgeOldRequestsAfterDays(int purgeOldRequestsAfterDays) {
    this.purgeOldRequestsAfterDays = purgeOldRequestsAfterDays;
  }

  public int getPurgeEveryHours() {
    return purgeEveryHours;
  }

  public void setPurgeEveryHours(int purgeEveryHours) {
    this.purgeEveryHours = purgeEveryHours;
  }
}

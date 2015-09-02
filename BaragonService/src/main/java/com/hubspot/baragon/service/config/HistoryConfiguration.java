package com.hubspot.baragon.service.config;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HistoryConfiguration {

  @JsonProperty("purgeOldRequests")
  boolean purgeOldRequests = false;

  @JsonProperty("purgeOldRequestsAfterDays")
  @Min(1)
  int purgeOldRequestsAfterDays = 7;

  @JsonProperty("purgeWhenDateNotFound")
  boolean purgeWhenDateNotFound = true;

  @JsonProperty("purgeEveryHours")
  @Min(1)
  int purgeEveryHours = 24;

  @JsonProperty("persistEveryMinutes")
  int persistEveryMinutes = 2;

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

  public int getPersistEveryMinutes() {
    return persistEveryMinutes;
  }

  public void setPersistEveryMinutes(int persistEveryMinutes) {
    this.persistEveryMinutes = persistEveryMinutes;
  }
}

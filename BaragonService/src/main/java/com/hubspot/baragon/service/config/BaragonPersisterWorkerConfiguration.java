package com.hubspot.baragon.service.config;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonPersisterWorkerConfiguration {

  private boolean enabled = true;

  @Min(1)
  private int intervalMs = 1000;

  @Min(0)
  private int initialDelayMs = 0;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getIntervalMs() {
    return intervalMs;
  }

  public void setIntervalMs(int intervalMs) {
    this.intervalMs = intervalMs;
  }

  public int getInitialDelayMs() {
    return initialDelayMs;
  }

  public void setInitialDelayMs(int initialDelayMs) {
    this.initialDelayMs = initialDelayMs;
  }
}

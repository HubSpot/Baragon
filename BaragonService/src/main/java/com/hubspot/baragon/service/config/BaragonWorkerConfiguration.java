package com.hubspot.baragon.service.config;

import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonWorkerConfiguration {
  private boolean enabled = true;

  @Min(1)
  private int intervalMs = 1000;

  @Min(0)
  private int initialDelayMs = 0;

  @Min(1)
  private int maxBatchSize = 50;

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

  public int getMaxBatchSize() {
    return maxBatchSize;
  }

  public void setMaxBatchSize(int maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
  }
}

package com.hubspot.baragon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestingConfiguration {
  @JsonProperty("enabled")
  private boolean enabled = false;

  @JsonProperty("applyDelayMs")
  private long applyDelayMs = 0;

  @JsonProperty("applyFailRate")
  private float applyFailRate = 0;

  @JsonProperty("revertDelayMs")
  public long revertDelayMs = 0;

  @JsonProperty("revertFailRate")
  public float revertFailRate = 0;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getApplyDelayMs() {
    return applyDelayMs;
  }

  public void setApplyDelayMs(long applyDelayMs) {
    this.applyDelayMs = applyDelayMs;
  }

  public long getRevertDelayMs() {
    return revertDelayMs;
  }

  public void setRevertDelayMs(long revertDelayMs) {
    this.revertDelayMs = revertDelayMs;
  }

  public float getApplyFailRate() {
    return applyFailRate;
  }

  public void setApplyFailRate(float applyFailRate) {
    this.applyFailRate = applyFailRate;
  }

  public float getRevertFailRate() {
    return revertFailRate;
  }

  public void setRevertFailRate(float revertFailRate) {
    this.revertFailRate = revertFailRate;
  }
}

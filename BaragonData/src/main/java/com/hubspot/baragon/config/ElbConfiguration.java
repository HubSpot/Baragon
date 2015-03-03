package com.hubspot.baragon.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElbConfiguration {
  @JsonProperty("enabled")
  private boolean enabled = false;

  @JsonProperty("awsAccessKeyId")
  private String awsAccessKeyId;

  @JsonProperty("awsAccessKeySecret")
  private String awsAccessKeySecret;

  @Min(5000)
  private int intervalMs = 10000;

  @Min(0)
  private int initialDelayMs = 0;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getAwsAccessKeyId() {
    return awsAccessKeyId;
  }

  public void setAwsAccessKeyId(String awsAccessKeyId) {
    this.awsAccessKeyId = awsAccessKeyId;
  }

  public String getAwsAccessKeySecret() {
    return awsAccessKeySecret;
  }

  public void setAwsAccessKeySecret(String awsAccessKeySecret) {
    this.awsAccessKeySecret = awsAccessKeySecret;
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

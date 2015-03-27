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

  @Min(60)
  private int intervalSeconds = 120;

  @Min(0)
  private int initialDelaySeconds = 0;

  @JsonProperty("removeLastHealthy")
  private boolean removeLastHealthy = false;

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

  public int getIntervalSeconds() {
    return intervalSeconds;
  }

  public void setIntervalSeconds(int intervalSeconds) {
    this.intervalSeconds = intervalSeconds;
  }

  public int getInitialDelaySeconds() {
    return initialDelaySeconds;
  }

  public void setInitialDelaySeconds(int initialDelaySeconds) {
    this.initialDelaySeconds = initialDelaySeconds;
  }

  public boolean canRemoveLastHealthy() {
    return removeLastHealthy;
  }

  public void setRemoveLastHealthy(boolean removeLastHealthy) {
    this.removeLastHealthy = removeLastHealthy;
  }
}

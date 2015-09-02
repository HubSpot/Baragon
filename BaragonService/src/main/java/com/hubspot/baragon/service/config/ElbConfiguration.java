package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElbConfiguration {
  @JsonProperty("enabled")
  private boolean enabled = false;

  @NotNull
  @JsonProperty("awsAccessKeyId")
  private String awsAccessKeyId;

  @NotNull
  @JsonProperty("awsAccessKeySecret")
  private String awsAccessKeySecret;

  @Min(60)
  private int intervalSeconds = 120;

  @Min(0)
  private int initialDelaySeconds = 0;

  @JsonProperty("removeLastHealthyEnabled")
  private boolean removeLastHealthyEnabled = false;

  @JsonProperty("removeKnownAgentEnabled")
  private boolean removeKnownAgentEnabled = false;

  @JsonProperty("removeKnownAgentMinutes")
  private int removeKnownAgentMinutes = 30;

  @JsonProperty("deregisterEnabled")
  private boolean deregisterEnabled = false;

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

  public boolean isRemoveLastHealthyEnabled() {
    return removeLastHealthyEnabled;
  }

  public void setRemoveLastHealthyEnabled(boolean removeLastHealthyEnabled) {
    this.removeLastHealthyEnabled = removeLastHealthyEnabled;
  }

  public boolean isRemoveKnownAgentEnabled() {
    return removeKnownAgentEnabled;
  }

  public void setRemoveKnownAgentEnabled(boolean removeKnownAgentEnabled) {
    this.removeKnownAgentEnabled = removeKnownAgentEnabled;
  }

  public boolean isDeregisterEnabled() {
    return deregisterEnabled;
  }

  public void setDeregisterEnabled(boolean deregisterEnabled) {
    this.deregisterEnabled = deregisterEnabled;
  }

  public int getRemoveKnownAgentMinutes() {
    return removeKnownAgentMinutes;
  }

  public void setRemoveKnownAgentMinutes(int removeKnownAgentMinutes) {
    this.removeKnownAgentMinutes = removeKnownAgentMinutes;
  }
}

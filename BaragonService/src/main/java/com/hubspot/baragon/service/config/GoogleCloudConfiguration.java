package com.hubspot.baragon.service.config;

public class GoogleCloudConfiguration {
  private boolean enabled = false;
  private String googleCredentialsFile = null;
  private String googleCredentials = null;
  private long defaultCheckInWaitTimeMs = 10000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getGoogleCredentialsFile() {
    return googleCredentialsFile;
  }

  public void setGoogleCredentialsFile(String googleCredentialsFile) {
    this.googleCredentialsFile = googleCredentialsFile;
  }

  public String getGoogleCredentials() {
    return googleCredentials;
  }

  public void setGoogleCredentials(String googleCredentials) {
    this.googleCredentials = googleCredentials;
  }

  public long getDefaultCheckInWaitTimeMs() {
    return defaultCheckInWaitTimeMs;
  }

  public void setDefaultCheckInWaitTimeMs(long defaultCheckInWaitTimeMs) {
    this.defaultCheckInWaitTimeMs = defaultCheckInWaitTimeMs;
  }
}

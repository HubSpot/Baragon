package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class HttpClientConfiguration {
  @JsonProperty("maxRequestRetry")
  private int maxRequestRetry = 5;

  @JsonProperty("requestTimeoutInMs")
  private int requestTimeoutInMs = 5000;

  @JsonProperty("connectionTimeoutInMs")
  private int connectionTimeoutInMs = 5000;

  @JsonProperty("userAgent")
  @NotEmpty
  private String userAgent = "Baragon/0.1 (+https://github.com/HubSpot/Baragon)";

  public int getMaxRequestRetry() {
    return maxRequestRetry;
  }

  public void setMaxRequestRetry(int maxRequestRetry) {
    this.maxRequestRetry = maxRequestRetry;
  }

  public int getRequestTimeoutInMs() {
    return requestTimeoutInMs;
  }

  public void setRequestTimeoutInMs(int requestTimeoutInMs) {
    this.requestTimeoutInMs = requestTimeoutInMs;
  }

  public int getConnectionTimeoutInMs() {
    return connectionTimeoutInMs;
  }

  public void setConnectionTimeoutInMs(int connectionTimeoutInMs) {
    this.connectionTimeoutInMs = connectionTimeoutInMs;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}

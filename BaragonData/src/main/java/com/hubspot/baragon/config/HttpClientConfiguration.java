package com.hubspot.baragon.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class HttpClientConfiguration {
  public static final int MAX_REQUEST_RETRY_DEFAULT = 5;
  public static final int REQUEST_TIMEOUT_IN_MS_DEFAULT = 10000;
  public static final int CONNECTION_TIMEOUT_IN_MS_DEFAULT = 5000;
  public static final String USER_AGENT_DEFAULT = "Baragon/0.1 (+https://github.com/HubSpot/Baragon)";

  @JsonProperty("maxRequestRetry")
  private int maxRequestRetry = MAX_REQUEST_RETRY_DEFAULT;

  @JsonProperty("requestTimeoutInMs")
  private int requestTimeoutInMs = REQUEST_TIMEOUT_IN_MS_DEFAULT;

  @JsonProperty("connectionTimeoutInMs")
  private int connectionTimeoutInMs = CONNECTION_TIMEOUT_IN_MS_DEFAULT;

  @JsonProperty("userAgent")
  @NotEmpty
  private String userAgent = USER_AGENT_DEFAULT;

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

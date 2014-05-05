package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonConfiguration extends Configuration {
  public static final String DEFAULT_AGENT_REQUEST_URI_FORMAT = "%s/request/%s";

  @JsonProperty("zookeeper")
  @NotNull
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("httpClient")
  private HttpClientConfiguration httpClientConfiguration = new HttpClientConfiguration();

  @JsonProperty("workerIntervalMs")
  private long workerIntervalMs = 1000;

  @JsonProperty("agentRequestUriFormat")
  @NotEmpty
  private String agentRequestUriFormat = DEFAULT_AGENT_REQUEST_URI_FORMAT;

  @JsonProperty("agentMaxAttempts")
  @Min(1)
  private int agentMaxAttempts = 5;

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public HttpClientConfiguration getHttpClientConfiguration() {
    return httpClientConfiguration;
  }

  public void setHttpClientConfiguration(HttpClientConfiguration httpClientConfiguration) {
    this.httpClientConfiguration = httpClientConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  public String getAgentRequestUriFormat() {
    return agentRequestUriFormat;
  }

  public void setAgentRequestUriFormat(String agentRequestUriFormat) {
    this.agentRequestUriFormat = agentRequestUriFormat;
  }

  public long getWorkerIntervalMs() {
    return workerIntervalMs;
  }

  public void setWorkerIntervalMs(long workerIntervalMs) {
    this.workerIntervalMs = workerIntervalMs;
  }

  public int getAgentMaxAttempts() {
    return agentMaxAttempts;
  }

  public void setAgentMaxAttempts(int agentMaxAttempts) {
    this.agentMaxAttempts = agentMaxAttempts;
  }
}

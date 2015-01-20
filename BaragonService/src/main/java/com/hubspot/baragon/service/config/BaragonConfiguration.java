package com.hubspot.baragon.service.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;

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

  @JsonProperty("startWorker")
  private Boolean startWorker = true;

  @JsonProperty("agentRequestUriFormat")
  @NotEmpty
  private String agentRequestUriFormat = DEFAULT_AGENT_REQUEST_URI_FORMAT;

  @JsonProperty("agentMaxAttempts")
  @Min(1)
  private int agentMaxAttempts = 5;

  @JsonProperty("auth")
  @Valid
  private AuthConfiguration authConfiguration = new AuthConfiguration();

  @JsonProperty("hostname")
  private String hostname;

  @JsonProperty("masterAuthKey")
  private String masterAuthKey;

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

  public Boolean getStartWorker() {
    return startWorker;
  }

  public void setStartWorker(Boolean startWorker) {
    this.startWorker = startWorker;
  }
  public int getAgentMaxAttempts() {
    return agentMaxAttempts;
  }

  public void setAgentMaxAttempts(int agentMaxAttempts) {
    this.agentMaxAttempts = agentMaxAttempts;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public AuthConfiguration getAuthConfiguration() {
    return authConfiguration;
  }

  public void setAuthConfiguration(AuthConfiguration authConfiguration) {
    this.authConfiguration = authConfiguration;
  }

  public String getMasterAuthKey() {
    return masterAuthKey;
  }

  public void setMasterAuthKey(String masterAuthKey) {
    this.masterAuthKey = masterAuthKey;
  }
}

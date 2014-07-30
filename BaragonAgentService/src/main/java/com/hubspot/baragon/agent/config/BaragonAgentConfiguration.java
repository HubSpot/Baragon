package com.hubspot.baragon.agent.config;

import io.dropwizard.Configuration;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.baragon.config.ZooKeeperConfiguration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonAgentConfiguration extends Configuration {
  public static final long DEFAULT_AGENT_LOCK_TIMEOUT_MS = 5000;
  public static final String DEFAULT_AGENT_BASE_URL_TEMPLATE = "http://%s:%d%s";
  public static final String DEFAULT_DATE_FORAMT = "yyyy-MM-dd hh:mm a";

  @JsonProperty("zookeeper")
  @NotNull
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("loadBalancerConfig")
  @NotNull
  private LoadBalancerConfiguration loadBalancerConfiguration;

  @JsonProperty("templates")
  @NotNull
  private List<TemplateConfiguration> templates = Collections.emptyList();

  @JsonProperty("hostname")
  private String hostname;

  @JsonProperty("agentLockTimeoutMs")
  @Min(0)
  private long agentLockTimeoutMs = DEFAULT_AGENT_LOCK_TIMEOUT_MS;

  @JsonProperty("baseUrlTemplate")
  @NotEmpty
  private String baseUrlTemplate = DEFAULT_AGENT_BASE_URL_TEMPLATE;

  @JsonProperty("defaultDateFormat")
  @NotEmpty
  private String defaultDateFormat = DEFAULT_DATE_FORAMT;

  @JsonProperty("testing")
  private TestingConfiguration testingConfiguration;

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

  public LoadBalancerConfiguration getLoadBalancerConfiguration() {
    return loadBalancerConfiguration;
  }

  public void setLoadBalancerConfiguration(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  public List<TemplateConfiguration> getTemplates() {
    return templates;
  }

  public void setTemplates(List<TemplateConfiguration> templates) {
    this.templates = templates;
  }

  public Optional<String> getHostname() {
    return Optional.fromNullable(Strings.emptyToNull(hostname));
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public long getAgentLockTimeoutMs() {
    return agentLockTimeoutMs;
  }

  public void setAgentLockTimeoutMs(long agentLockTimeoutMs) {
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public TestingConfiguration getTestingConfiguration() {
    return testingConfiguration;
  }

  public void setTestingConfiguration(TestingConfiguration testingConfiguration) {
    this.testingConfiguration = testingConfiguration;
  }

  public String getBaseUrlTemplate() {
    return baseUrlTemplate;
  }

  public void setBaseUrlTemplate(String baseUrlTemplate) {
    this.baseUrlTemplate = baseUrlTemplate;
  }

  public String getDefaultDateFormat() {
    return defaultDateFormat;
  }

  public void setDefaultDateFormat(String defaultDateFormat) {
    this.defaultDateFormat = defaultDateFormat;
  }
}

package com.hubspot.baragon.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.config.TemplateConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonAgentConfiguration extends Configuration {
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

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
}

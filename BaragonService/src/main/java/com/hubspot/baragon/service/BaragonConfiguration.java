package com.hubspot.baragon.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import io.dropwizard.Configuration;

public class BaragonConfiguration extends Configuration {
  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }
}

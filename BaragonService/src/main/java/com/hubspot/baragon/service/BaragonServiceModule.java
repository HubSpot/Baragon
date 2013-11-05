package com.hubspot.baragon.service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;


public class BaragonServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
  }

  @Provides
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }
}
package com.hubspot.baragon.auth;

import org.glassfish.jersey.server.ResourceConfig;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonAuthBundle implements Bundle {
  @Override
  public void initialize(Bootstrap<?> bootstrap) {

  }

  @Override
  public void run(Environment environment) {
    ResourceConfig resourceConfig = environment.jersey().getResourceConfig();
    resourceConfig.register(BaragonAuthFeature.class);
  }
}

package com.hubspot.baragon.auth;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.ResourceConfig;

public class BaragonAuthBundle implements Bundle {

  @Override
  public void initialize(Bootstrap<?> bootstrap) {}

  @Override
  public void run(Environment environment) {
    ResourceConfig resourceConfig = environment.jersey().getResourceConfig();
    resourceConfig.register(BaragonAuthFeature.class);
  }
}

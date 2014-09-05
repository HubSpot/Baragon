package com.hubspot.baragon.auth;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonAuthBundle implements Bundle {
  @Override
  public void initialize(Bootstrap<?> bootstrap) {

  }

  @Override
  public void run(Environment environment) {
    environment.jersey().getResourceConfig().getResourceFilterFactories().add(BaragonAuthResourceFilterFactory.class);
  }
}

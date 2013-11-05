package com.hubspot.baragon.service;

import com.google.inject.Stage;
import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonService extends Application<BaragonConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonConfiguration> bootstrap) {
    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.<BaragonConfiguration>newBuilder()
        .addModule(new BaragonServiceModule())
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(BaragonConfiguration.class)
        .build(Stage.DEVELOPMENT);
    bootstrap.addBundle(guiceBundle);
  }

  @Override
  public void run(BaragonConfiguration configuration, Environment environment) throws Exception {}

  public static void main(String[] args) throws Exception {
    new BaragonService().run(args);
  }

}
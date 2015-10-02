package com.hubspot.baragon.agent;

import com.google.inject.Stage;
import com.hubspot.baragon.agent.bundles.CorsBundle;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonAgentService extends Application<BaragonAgentConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonAgentConfiguration> bootstrap) {
    GuiceBundle<BaragonAgentConfiguration> guiceBundle = GuiceBundle.<BaragonAgentConfiguration>newBuilder()
        .addModule(new BaragonAgentServiceModule())
        .addModule(new MetricsInstrumentationModule(bootstrap.getMetricRegistry()))
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(BaragonAgentConfiguration.class)
        .build(Stage.DEVELOPMENT);

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new BaragonAuthBundle());
  }

  @Override
  public void run(BaragonAgentConfiguration configuration, Environment environment) throws Exception {}

  public static void main(String[] args) throws Exception {
    new BaragonAgentService().run(args);
  }

}

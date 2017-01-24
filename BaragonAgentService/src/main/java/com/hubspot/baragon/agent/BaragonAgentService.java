package com.hubspot.baragon.agent;

import com.hubspot.baragon.agent.bundles.CorsBundle;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonAgentService extends Application<BaragonAgentConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonAgentConfiguration> bootstrap) {
    GuiceBundle<BaragonAgentConfiguration> guiceBundle = GuiceBundle.defaultBuilder(BaragonAgentConfiguration.class)
        .modules(new BaragonAgentServiceModule())
        .modules(new MetricsInstrumentationModule(bootstrap.getMetricRegistry()))
        .build();

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new BaragonAuthBundle());
  }

  @Override
  public void run(BaragonAgentConfiguration configuration, Environment environment) throws Exception { }

  public static void main(String[] args) throws Exception {
    new BaragonAgentService().run(args);
  }

}

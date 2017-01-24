package com.hubspot.baragon.service;

import com.hubspot.baragon.service.bundles.CorsBundle;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.resources.BaragonResourcesModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class BaragonService extends Application<BaragonConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonConfiguration> bootstrap) {
    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.defaultBuilder(BaragonConfiguration.class)
        .modules(new BaragonServiceModule())
        .modules(new MetricsInstrumentationModule(bootstrap.getMetricRegistry()))
        .modules(new BaragonResourcesModule())
        .enableGuiceEnforcer(false) // TODO: Fix our modules so we don't need this anymore
        .build();

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new ViewBundle<BaragonConfiguration>());
    bootstrap.addBundle(new AssetsBundle("/assets/static/", "/static/"));
  }

  @Override
  public void run(BaragonConfiguration configuration, Environment environment) throws Exception {
  }

  public static void main(String[] args) throws Exception {
    new BaragonService().run(args);
  }
}

package com.hubspot.baragon.service;

import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.hubspot.baragon.service.bundles.CorsBundle;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.resources.BaragonResourcesModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.hubspot.baragon.service.config.MergingConfigProvider;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class BaragonService extends Application<BaragonConfiguration> {
  private static final String BARAGON_DEFAULT_CONFIG_LOCATION = "baragonDefaultConfiguration";

  @Override
  public void initialize(Bootstrap<BaragonConfiguration> bootstrap) {
    if (!Strings.isNullOrEmpty(System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION))) {
      bootstrap.setConfigurationSourceProvider(
          new MergingConfigProvider(
              bootstrap.getConfigurationSourceProvider(),
              System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION),
              bootstrap.getObjectMapper(),
              new YAMLFactory()));
    }

    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.defaultBuilder(BaragonConfiguration.class)
        .modules(new BaragonServiceModule())
        .modules(new MetricsInstrumentationModule(bootstrap.getMetricRegistry()))
        .modules(new BaragonResourcesModule())
        .build();

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(new BaragonAuthBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new ViewBundle<>());
    bootstrap.addBundle(new AssetsBundle("/assets/static/", "/static/"));
  }

  @Override
  public void run(BaragonConfiguration configuration, Environment environment) throws Exception {
  }

  public static void main(String[] args) throws Exception {
    new BaragonService().run(args);
  }
}

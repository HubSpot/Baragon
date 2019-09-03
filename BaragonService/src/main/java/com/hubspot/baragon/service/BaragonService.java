package com.hubspot.baragon.service;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.hubspot.baragon.service.bundles.CorsBundle;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.MergingConfigProvider;
import com.hubspot.baragon.service.resources.BaragonResourcesModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class BaragonService<T extends BaragonConfiguration> extends Application<T> {
  private static final String BARAGON_DEFAULT_CONFIG_LOCATION = "baragonDefaultConfiguration";

  @Override
  public void initialize(Bootstrap<T> bootstrap) {
    if (!Strings.isNullOrEmpty(System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION))) {
      bootstrap.setConfigurationSourceProvider(
          new MergingConfigProvider(
              bootstrap.getConfigurationSourceProvider(),
              System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION),
              bootstrap.getObjectMapper(),
              new YAMLFactory()));
    }
    final Iterable<? extends Module> additionalModules = checkNotNull(getGuiceModules(bootstrap), "getGuiceModules() returned null");

    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.defaultBuilder(BaragonConfiguration.class)
        .modules(new BaragonServiceModule())
        .modules(new BaragonResourcesModule())
        .modules(getObjectMapperModule())
        .modules(additionalModules)
        .enableGuiceEnforcer(false)
        .stage(getGuiceStage())
        .build();

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(new BaragonAuthBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new ViewBundle<>());
    bootstrap.addBundle(new AssetsBundle("/assets/static/", "/static/"));
  }

  public Stage getGuiceStage() {
    return Stage.PRODUCTION;
  }

  /**
   * Guice modules used in addition to the modules required by Baragon. This is an extension point when embedding
   * Baragon into a custom service.
   */
  public Iterable<? extends Module> getGuiceModules(Bootstrap<T> bootstrap) {
    return ImmutableList.of();
  }

  public Module getObjectMapperModule() {
    return (binder) -> {
      final ObjectMapper objectMapper = new ObjectMapper();

      objectMapper.registerModule(new GuavaModule());
      objectMapper.registerModule(new Jdk8Module());

      binder.bind(ObjectMapper.class).toInstance(objectMapper);
    };
  }

  @Override
  public void run(T configuration, Environment environment) throws Exception {
  }

  public static void main(String[] args) throws Exception {
    new BaragonService<BaragonConfiguration>().run(args);
  }
}

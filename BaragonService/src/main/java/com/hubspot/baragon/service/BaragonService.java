package com.hubspot.baragon.service;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.migrations.MigrationsBundle;

import com.google.inject.Stage;
import com.hubspot.baragon.auth.BaragonAuthUpdater;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.bundles.CorsBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;

public class BaragonService extends Application<BaragonConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonConfiguration> bootstrap) {
    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.<BaragonConfiguration>newBuilder()
        .addModule(new BaragonServiceModule())
        .enableAutoConfig(getClass().getPackage().getName(), BaragonAuthUpdater.class.getPackage().getName())
        .setConfigClass(BaragonConfiguration.class)
        .build(Stage.DEVELOPMENT);

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(guiceBundle);
    bootstrap.addBundle(new ViewBundle());
    bootstrap.addBundle(new AssetsBundle("/assets/static/", "/static/"));
    bootstrap.addBundle(new MigrationsBundle<BaragonConfiguration>() {
      @Override
      public DataSourceFactory getDataSourceFactory(final BaragonConfiguration configuration) {
        return configuration.getDatabaseConfiguration().get();
      }
    });
  }

  @Override
  public void run(BaragonConfiguration configuration, Environment environment) throws Exception {
  }

  public static void main(String[] args) throws Exception {
    new BaragonService().run(args);
  }
}

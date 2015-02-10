package com.hubspot.baragon.service;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.google.inject.Stage;
import com.hubspot.baragon.auth.BaragonAuthUpdater;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.dropwizard.guice.GuiceBundle;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import java.util.EnumSet;

public class BaragonService extends Application<BaragonConfiguration> {

  @Override
  public void initialize(Bootstrap<BaragonConfiguration> bootstrap) {
    GuiceBundle<BaragonConfiguration> guiceBundle = GuiceBundle.<BaragonConfiguration>newBuilder()
        .addModule(new BaragonServiceModule())
        .enableAutoConfig(getClass().getPackage().getName(), BaragonAuthUpdater.class.getPackage().getName())
        .setConfigClass(BaragonConfiguration.class)
        .build(Stage.DEVELOPMENT);

    bootstrap.addBundle(guiceBundle);
  }

  @Override
  public void run(BaragonConfiguration configuration, Environment environment) throws Exception {
    final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
  }

  public static void main(String[] args) throws Exception {
    new BaragonService().run(args);
  }

}
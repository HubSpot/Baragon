package com.hubspot.baragon.agent;

import com.google.inject.Stage;
import com.hubspot.baragon.agent.bundles.CorsBundle;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managed.LifecycleHelper;
import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.eclipse.jetty.server.Server;

public class BaragonAgentService extends Application<BaragonAgentConfiguration> {

  private GuiceBundle<BaragonAgentConfiguration> guiceBundle;

  @Override
  public void initialize(Bootstrap<BaragonAgentConfiguration> bootstrap) {
    guiceBundle = GuiceBundle.<BaragonAgentConfiguration>newBuilder()
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
  public void run(BaragonAgentConfiguration configuration, Environment environment) throws Exception {
    environment.lifecycle().addServerLifecycleListener(new ServerProvider());
    environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {
      @Override
      public void serverStarted(Server server) {
        guiceBundle.getInjector().getInstance(LifecycleHelper.class).checkStateNodeVersion();
      }
    });
  }

  public static void main(String[] args) throws Exception {
    new BaragonAgentService().run(args);
  }

}

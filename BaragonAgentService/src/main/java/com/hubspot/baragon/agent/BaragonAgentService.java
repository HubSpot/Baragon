package com.hubspot.baragon.agent;

import org.eclipse.jetty.server.Server;

import com.hubspot.baragon.agent.bundles.CorsBundle;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managed.LifecycleHelper;
import com.hubspot.baragon.auth.BaragonAuthBundle;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import io.dropwizard.Application;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BaragonAgentService extends Application<BaragonAgentConfiguration> {

  private GuiceBundle<BaragonAgentConfiguration> guiceBundle;

  @Override
  public void initialize(Bootstrap<BaragonAgentConfiguration> bootstrap) {
    guiceBundle = GuiceBundle.defaultBuilder(BaragonAgentConfiguration.class)
        .modules(new BaragonAgentServiceModule())
        .modules(new MetricsInstrumentationModule(bootstrap.getMetricRegistry()))
        .build();

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

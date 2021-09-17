package com.hubspot.baragon.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.TestAgentManager;
import com.hubspot.dropwizard.guicier.DropwizardModule;
import io.dropwizard.setup.Environment;
import org.apache.curator.test.TestingServer;
import org.slf4j.LoggerFactory;

public class BaragonServiceTestModule extends AbstractModule {
  private final TestingServer ts;
  private final DropwizardModule dropwizardModule;
  private final ObjectMapper om = getObjectMapper();
  private final Environment environment = new Environment(
    "test-env",
    om,
    null,
    new MetricRegistry(),
    null
  );

  public BaragonServiceTestModule() throws Exception {
    this.ts = new TestingServer();

    this.dropwizardModule = new DropwizardModule(environment);

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(
      Level.toLevel(System.getProperty("baragon.test.log.level", "WARN"))
    );

    Logger hsLogger = context.getLogger("com.hubspot");
    hsLogger.setLevel(
      Level.toLevel(System.getProperty("baragon.test.log.level.for.com.hubspot", "INFO"))
    );
  }

  @Override
  protected void configure() {
    bind(TestingServer.class).toInstance(ts);
    BaragonConfiguration baragonConfiguration = getBaragonConfigurationForTestingServer(
      ts
    );
    bind(BaragonConfiguration.class).toInstance(baragonConfiguration);
    BaragonServiceModule serviceModule = new BaragonServiceModule();
    serviceModule.setConfiguration(baragonConfiguration);
    serviceModule.setEnvironment(environment);
    binder()
      .install(
        Modules
          .override(serviceModule)
          .with(
            new Module() {

              @Override
              public void configure(Binder binder) {
                binder.bind(ObjectMapper.class).toInstance(om);
                binder.bind(Environment.class).toInstance(environment);
                bind(TestAgentManager.class).in(Scopes.SINGLETON);
                binder.bind(AgentManager.class).to(TestAgentManager.class);
              }
            }
          )
      );
  }

  public Injector getInjector() throws Exception {
    return Guice.createInjector(Stage.PRODUCTION, dropwizardModule, this);
  }

  private static ObjectMapper getObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new Jdk8Module());

    return objectMapper;
  }

  private static BaragonConfiguration getBaragonConfigurationForTestingServer(
    final TestingServer ts
  ) {
    BaragonConfiguration config = new BaragonConfiguration();
    ZooKeeperConfiguration zookeeperConfiguration = new ZooKeeperConfiguration();
    zookeeperConfiguration.setQuorum(ts.getConnectString());
    zookeeperConfiguration.setConnectTimeoutMillis(1000);
    zookeeperConfiguration.setRetryBaseSleepTimeMilliseconds(1000);
    zookeeperConfiguration.setRetryMaxTries(1);
    zookeeperConfiguration.setSessionTimeoutMillis(1000);
    zookeeperConfiguration.setZkNamespace("baragontesting");
    config.setZooKeeperConfiguration(zookeeperConfiguration);

    return config;
  }
}

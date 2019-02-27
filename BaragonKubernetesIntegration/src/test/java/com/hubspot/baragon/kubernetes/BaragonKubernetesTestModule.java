package com.hubspot.baragon.kubernetes;

import com.google.inject.AbstractModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.KubernetesIntegrationConfiguration;

public class BaragonKubernetesTestModule extends AbstractModule {

  @Override
  protected void configure() {
    BaragonConfiguration baragonConfiguration = new BaragonConfiguration();
    baragonConfiguration.setKubernetesIntegrated(KubernetesIntegrationConfiguration.builder().build());
    install(new BaragonKubernetesIntegrationModule(baragonConfiguration));

    setLoggingLevel(ch.qos.logback.classic.Level.DEBUG);
  }

  public static void setLoggingLevel(ch.qos.logback.classic.Level level) {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    root.setLevel(level);
  }
}

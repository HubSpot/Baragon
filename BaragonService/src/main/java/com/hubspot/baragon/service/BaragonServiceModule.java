package com.hubspot.baragon.service;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.utils.JavaUtils;


public class BaragonServiceModule extends AbstractModule {
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";
  public static final String BARAGON_SERVICE_LEADER_LATCH = "baragon.service.leaderLatch";


  public static final String BARAGON_SERVICE_HTTP_PORT = "baragon.service.http.port";
  public static final String BARAGON_SERVICE_HOSTNAME = "baragon.service.hostname";

  public static final String BARAGON_MASTER_AUTH_KEY = "baragon.master.auth.key";

  @Override
  protected void configure() {
    install(new BaragonDataModule());
  }

  @Provides
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }

  @Provides
  public HttpClientConfiguration provideHttpClientConfiguration(BaragonConfiguration configuration) {
    return configuration.getHttpClientConfiguration();
  }

  @Provides
  @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT)
  public String provideAgentUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentRequestUriFormat();
  }

  @Provides
  @Named(BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS)
  public Integer provideAgentMaxAttempts(BaragonConfiguration configuration) {
    return configuration.getAgentMaxAttempts();
  }

  @Provides
  public AuthConfiguration providesAuthConfiguration(BaragonConfiguration configuration) {
    return configuration.getAuthConfiguration();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(1);
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START)
  public AtomicLong providesWorkerLastStartAt() {
    return new AtomicLong();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_HTTP_PORT)
  public int providesHttpPortProperty(BaragonConfiguration config) {
    SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

    return httpFactory.getPort();
  }

  @Provides
  @Named(BARAGON_SERVICE_HOSTNAME)
  public String providesHostnameProperty(BaragonConfiguration config) throws Exception {
    return Strings.isNullOrEmpty(config.getHostname()) ? JavaUtils.getHostAddress() : config.getHostname();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_LEADER_LATCH)
  public LeaderLatch providesServiceLeaderLatch(BaragonConfiguration config,
                                                BaragonWorkerDatastore datastore,
                                                @Named(BARAGON_SERVICE_HTTP_PORT) int httpPort,
                                                @Named(BARAGON_SERVICE_HOSTNAME) String hostname) {
    final String appRoot = ((SimpleServerFactory)config.getServerFactory()).getApplicationContextPath();
    final String baseUri = String.format("http://%s:%s%s", hostname, httpPort, appRoot);

    return datastore.createLeaderLatch(baseUri);
  }

  @Provides
  @Named(BARAGON_MASTER_AUTH_KEY)
  public String providesMasterAuthKey(BaragonConfiguration configuration) {
    return configuration.getMasterAuthKey();
  }
}
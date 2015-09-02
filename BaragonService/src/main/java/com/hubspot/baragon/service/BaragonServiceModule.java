package com.hubspot.baragon.service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.ProvisionException;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.history.BaragonMappers;
import com.hubspot.baragon.service.history.HistoryManager;
import com.hubspot.baragon.service.history.JDBIHistoryManager;
import com.hubspot.baragon.service.history.NoopHistoryManager;
import com.hubspot.baragon.service.listeners.AbstractLatchListener;
import com.hubspot.baragon.service.listeners.ElbSyncWorkerListener;
import com.hubspot.baragon.service.listeners.HistoryPersisterListener;
import com.hubspot.baragon.service.listeners.RequestWorkerListener;
import com.hubspot.baragon.service.worker.HistoryPersisterWorker;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import io.dropwizard.setup.Environment;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.history.HistoryJDBI;
import com.hubspot.baragon.utils.JavaUtils;
import org.skife.jdbi.v2.DBI;

public class BaragonServiceModule extends AbstractModule {
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";

  public static final String BARAGON_SERVICE_HTTP_PORT = "baragon.service.http.port";
  public static final String BARAGON_SERVICE_HOSTNAME = "baragon.service.hostname";
  public static final String BARAGON_SERVICE_HTTP_CLIENT = "baragon.service.http.client";

  public static final String BARAGON_MASTER_AUTH_KEY = "baragon.master.auth.key";

  public static final String BARAGON_URI_BASE = "_baragon_uri_base";

  public static final String BARAGON_AWS_ELB_CLIENT = "baragon.aws.elb.client";

  @Override
  protected void configure() {
    install(new BaragonDataModule());

    Multibinder<AbstractLatchListener> latchBinder = Multibinder.newSetBinder(binder(), AbstractLatchListener.class);
    latchBinder.addBinding().to(RequestWorkerListener.class);
    latchBinder.addBinding().to(ElbSyncWorkerListener.class);
    latchBinder.addBinding().to(HistoryPersisterListener.class);
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
  @Named(BaragonDataModule.BARAGON_AGENT_REQUEST_TIMEOUT_MS)
  public Long provideAgentMaxRequestTime(BaragonConfiguration configuration) {
    return configuration.getAgentRequestTimeoutMs();
  }

  @Provides
  public AuthConfiguration providesAuthConfiguration(BaragonConfiguration configuration) {
    return configuration.getAuthConfiguration();
  }

  @Provides
  public Optional<ElbConfiguration> providesElbConfiguration(BaragonConfiguration configuration) {
    return configuration.getElbConfiguration();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(3);
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START)
  public AtomicLong providesWorkerLastStartAt() {
    return new AtomicLong();
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START)
  public AtomicLong providesElbWorkerLastStartAt() {
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
  @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH)
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

  @Provides
  @Named(BARAGON_URI_BASE)
  String getSingularityUriBase(final BaragonConfiguration configuration) {
    final String singularityUiPrefix = configuration.getUiConfiguration().getBaseUrl().or(((SimpleServerFactory) configuration.getServerFactory()).getApplicationContextPath());
    return (singularityUiPrefix.endsWith("/")) ?  singularityUiPrefix.substring(0, singularityUiPrefix.length() - 1) : singularityUiPrefix;
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_HTTP_CLIENT)
  public AsyncHttpClient providesHttpClient(HttpClientConfiguration config) {
    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

    builder.setMaxRequestRetry(config.getMaxRequestRetry());
    builder.setRequestTimeoutInMs(config.getRequestTimeoutInMs());
    builder.setFollowRedirects(true);
    builder.setConnectionTimeoutInMs(config.getConnectionTimeoutInMs());
    builder.setUserAgent(config.getUserAgent());

    return new AsyncHttpClient(builder.build());
  }

  @Provides
  @Named(BARAGON_AWS_ELB_CLIENT)
  public AmazonElasticLoadBalancingClient providesAwsElbClient(Optional<ElbConfiguration> configuration) {
    if (configuration.isPresent() && configuration.get().getAwsAccessKeyId() != null && configuration.get().getAwsAccessKeySecret() != null) {
      return new AmazonElasticLoadBalancingClient(new BasicAWSCredentials(configuration.get().getAwsAccessKeyId(), configuration.get().getAwsAccessKeySecret()));
    } else {
      return new AmazonElasticLoadBalancingClient();
    }
  }

  @Provides
  @Singleton
  public HistoryManager providesHistoryManager(ObjectMapper objectMapper, Environment environment, BaragonConfiguration configuration) {
    if (configuration.getDatabaseConfiguration().isPresent()) {
      try {
        DataSourceFactory dataSourceFactory = configuration.getDatabaseConfiguration().get();
        DBI dbi = new DBIFactory().build(environment, dataSourceFactory, "db");
        dbi.registerMapper(new BaragonMappers.BaragonBytesMapper());
        dbi.registerMapper(new BaragonMappers.BaragonRequestIdMapper());
        return new JDBIHistoryManager(objectMapper, dbi.onDemand(HistoryJDBI.class));
      } catch (ClassNotFoundException e) {
        throw new ProvisionException("while instantiating DBI", e);
      }
    } else {
      return new NoopHistoryManager();
    }
  }
}

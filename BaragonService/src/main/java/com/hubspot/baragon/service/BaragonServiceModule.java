package com.hubspot.baragon.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.data.BaragonConnectionStateListener;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.BaragonServiceDWSettings;
import com.hubspot.baragon.service.config.EdgeCacheConfiguration;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.config.SentryConfiguration;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.edgecache.cloudflare.CloudflareEdgeCache;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareClient;
import com.hubspot.baragon.service.elb.ApplicationLoadBalancer;
import com.hubspot.baragon.service.elb.ClassicLoadBalancer;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.gcloud.GoogleCloudManager;
import com.hubspot.baragon.service.healthcheck.ZooKeeperHealthcheck;
import com.hubspot.baragon.service.listeners.AbstractLatchListener;
import com.hubspot.baragon.service.listeners.ElbSyncWorkerListener;
import com.hubspot.baragon.service.listeners.RequestPurgingListener;
import com.hubspot.baragon.service.listeners.RequestWorkerListener;
import com.hubspot.baragon.service.managed.BaragonExceptionNotifierManaged;
import com.hubspot.baragon.service.managed.BaragonGraphiteReporterManaged;
import com.hubspot.baragon.service.managed.BaragonManaged;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.ElbManager;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.service.managers.ServiceManager;
import com.hubspot.baragon.service.managers.StatusManager;
import com.hubspot.baragon.service.resources.BaragonResourcesModule;
import com.hubspot.baragon.service.worker.BaragonElbSyncWorker;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;
import com.hubspot.baragon.service.worker.RequestPurgingWorker;
import com.hubspot.baragon.utils.JavaUtils;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.SimpleServerFactory;

public class BaragonServiceModule extends DropwizardAwareModule<BaragonConfiguration> {
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";

  public static final String BARAGON_SERVICE_DW_CONFIG = "baragon.service.port";
  public static final String BARAGON_SERVICE_HOSTNAME = "baragon.service.hostname";
  public static final String BARAGON_SERVICE_LOCAL_HOSTNAME = "baragon.service.local.hostname";
  public static final String BARAGON_SERVICE_HTTP_CLIENT = "baragon.service.http.client";

  public static final String BARAGON_MASTER_AUTH_KEY = "baragon.master.auth.key";

  public static final String BARAGON_URI_BASE = "baragon.uri.base";

  public static final String BARAGON_AWS_ELB_CLIENT_V1 = "baragon.aws.elb.client.v1";
  public static final String BARAGON_AWS_ELB_CLIENT_V2 = "baragon.aws.elb.client.v2";

  public static final String GOOGLE_CLOUD_COMPUTE_SERVICE = "baragon.google.cloud.compute.service";

  @Override
  public void configure(Binder binder) {
    binder.requireExplicitBindings();
    binder.requireExactBindingAnnotations();
    binder.requireAtInjectOnConstructors();

    binder.install(new BaragonDataModule());
    binder.install(new BaragonResourcesModule());

    // Healthcheck
    binder.bind(ZooKeeperHealthcheck.class).in(Scopes.SINGLETON);
    binder.bind(BaragonExceptionNotifier.class).in(Scopes.SINGLETON);

    // Managed
    binder.bind(BaragonExceptionNotifierManaged.class).in(Scopes.SINGLETON);
    binder.bind(BaragonGraphiteReporterManaged.class).in(Scopes.SINGLETON);
    binder.bind(BaragonManaged.class).in(Scopes.SINGLETON);

    // Managers
    binder.bind(AgentManager.class).in(Scopes.SINGLETON);
    binder.bind(ElbManager.class).in(Scopes.SINGLETON);
    binder.bind(RequestManager.class).in(Scopes.SINGLETON);
    binder.bind(ServiceManager.class).in(Scopes.SINGLETON);
    binder.bind(StatusManager.class).in(Scopes.SINGLETON);

    binder.bind(GoogleCloudManager.class).in(Scopes.SINGLETON);

    // Edge Cache
    binder.bind(CloudflareEdgeCache.class);
    binder.bind(CloudflareClient.class);
    binder.bind(EdgeCache.class).to(getConfiguration().getEdgeCacheConfiguration().getEdgeCache().getEdgeCacheClass());

    // Workers
    binder.bind(BaragonElbSyncWorker.class).in(Scopes.SINGLETON);
    binder.bind(BaragonRequestWorker.class).in(Scopes.SINGLETON);
    binder.bind(RequestPurgingWorker.class).in(Scopes.SINGLETON);

    binder.bind(ClassicLoadBalancer.class);
    binder.bind(ApplicationLoadBalancer.class);

    Multibinder<AbstractLatchListener> latchBinder = Multibinder.newSetBinder(binder, AbstractLatchListener.class);
    latchBinder.addBinding().to(RequestWorkerListener.class).in(Scopes.SINGLETON);
    latchBinder.addBinding().to(ElbSyncWorkerListener.class).in(Scopes.SINGLETON);
    latchBinder.addBinding().to(RequestPurgingListener.class).in(Scopes.SINGLETON);
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
  @Named(BaragonDataModule.BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT)
  public String provideAgentBatchUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentBatchRequestUriFormat();
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
  public EdgeCacheConfiguration providesEdgeCacheConfiguration(BaragonConfiguration configuration) {
    return configuration.getEdgeCacheConfiguration();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(4);
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
  @Named(BARAGON_SERVICE_DW_CONFIG)
  public BaragonServiceDWSettings providesHttpPortProperty(BaragonConfiguration config) {
    Integer port = null;
    String contextPath;
    // Currently we only look for an http connector, not an https connector
    if (config.getServerFactory() instanceof SimpleServerFactory) {
      SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
      HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();
      port = httpFactory.getPort();
      contextPath = simpleServerFactory.getApplicationContextPath();
    } else {
      DefaultServerFactory defaultServerFactory = (DefaultServerFactory) config.getServerFactory();
      contextPath = defaultServerFactory.getApplicationContextPath();
      for (ConnectorFactory connectorFactory : defaultServerFactory.getApplicationConnectors()) {
        // Only looking for http connectors for now
        if (connectorFactory instanceof HttpConnectorFactory && !(connectorFactory instanceof HttpsConnectorFactory)) {
          HttpConnectorFactory httpFactory = (HttpConnectorFactory) connectorFactory;
          port = httpFactory.getPort();
        }
      }
    }
    if (port == null) {
      throw new RuntimeException("Could not determine http port");
    }
    return new BaragonServiceDWSettings(port, contextPath);
  }

  @Provides
  @Named(BARAGON_SERVICE_HOSTNAME)
  public String providesHostnameProperty(BaragonConfiguration config) throws Exception {
    return Strings.isNullOrEmpty(config.getHostname()) ? JavaUtils.getHostAddress() : config.getHostname();
  }

  @Provides
  @Named(BARAGON_SERVICE_LOCAL_HOSTNAME)
  public String providesLocalHostnameProperty(BaragonConfiguration config) {
    if (!Strings.isNullOrEmpty(config.getHostname())) {
      return config.getHostname();
    }

    try {
      final InetAddress addr = InetAddress.getLocalHost();

      return addr.getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("No local hostname found, unable to start without functioning local networking (or configured hostname)", e);
    }
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH)
  public LeaderLatch providesServiceLeaderLatch(BaragonConfiguration config,
                                                BaragonWorkerDatastore datastore,
                                                @Named(BARAGON_SERVICE_DW_CONFIG) BaragonServiceDWSettings dwConfig,
                                                @Named(BARAGON_SERVICE_HOSTNAME) String hostname) {
    final String baseUri = String.format("http://%s:%s%s", hostname, dwConfig.getPort(), dwConfig.getContextPath());

    return datastore.createLeaderLatch(baseUri);
  }

  @Provides
  @Named(BARAGON_MASTER_AUTH_KEY)
  public String providesMasterAuthKey(BaragonConfiguration configuration) {
    return configuration.getMasterAuthKey();
  }

  @Provides
  @Named(BARAGON_URI_BASE)
  String getBaragonUriBase(final BaragonConfiguration configuration,
                           @Named(BARAGON_SERVICE_DW_CONFIG) BaragonServiceDWSettings dwSettings) {
    final String baragonUiPrefix = configuration.getUiConfiguration().getBaseUrl().or(dwSettings.getContextPath());
    return (baragonUiPrefix.endsWith("/")) ?  baragonUiPrefix.substring(0, baragonUiPrefix.length() - 1) : baragonUiPrefix;
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
  @Named(BARAGON_AWS_ELB_CLIENT_V1)
  public AmazonElasticLoadBalancingClient providesAwsElbClientV1(Optional<ElbConfiguration> configuration) {
    AmazonElasticLoadBalancingClient elbClient;
    if (configuration.isPresent() && configuration.get().getAwsAccessKeyId() != null && configuration.get().getAwsAccessKeySecret() != null) {
      elbClient = new AmazonElasticLoadBalancingClient(new BasicAWSCredentials(configuration.get().getAwsAccessKeyId(), configuration.get().getAwsAccessKeySecret()));
    } else {
      elbClient = new AmazonElasticLoadBalancingClient();
    }

    if (configuration.isPresent() && configuration.get().getAwsEndpoint().isPresent()) {
      elbClient.setEndpoint(configuration.get().getAwsEndpoint().get());
    }
    if (configuration.isPresent() && configuration.get().getAwsRegion().isPresent()) {
      elbClient.configureRegion(Regions.fromName(configuration.get().getAwsRegion().get()));
    }

    return elbClient;
  }

  @Provides
  @Singleton
  @Named(GOOGLE_CLOUD_COMPUTE_SERVICE)
  public Optional<Compute> provideComputeService(BaragonConfiguration config) throws Exception {
    if (!config.getGoogleCloudConfiguration().isEnabled()) {
      return Optional.absent();
    }
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    GoogleCredential credential = null;

    if (config.getGoogleCloudConfiguration().getGoogleCredentialsFile() != null) {
      File credentialsFile = new File(config.getGoogleCloudConfiguration().getGoogleCredentialsFile());
      credential = GoogleCredential.fromStream(
          new FileInputStream(credentialsFile)
      );
    } else if (config.getGoogleCloudConfiguration().getGoogleCredentials() != null) {
      credential = GoogleCredential.fromStream(
          new ByteArrayInputStream(config.getGoogleCloudConfiguration().getGoogleCredentials().getBytes("UTF-8"))
      );
    } else {
      throw new RuntimeException("Must specify googleCloudCredentials or googleCloudCredentialsFile when using google cloud api");
    }

    if (credential.createScopedRequired()) {
      credential =
          credential.createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }

    return Optional.of(new Compute.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("BaragonService")
        .build());
  }

  @Provides
  @Named(BARAGON_AWS_ELB_CLIENT_V2)
  public com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient providesAwsElbClientV2(Optional<ElbConfiguration> configuration) {
    com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient elbClient;
    if (configuration.isPresent() && configuration.get().getAwsAccessKeyId() != null && configuration.get().getAwsAccessKeySecret() != null) {
      elbClient = new com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient(new BasicAWSCredentials(configuration.get().getAwsAccessKeyId(), configuration.get().getAwsAccessKeySecret()));
    } else {
      elbClient = new com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient();
    }

    if (configuration.isPresent() && configuration.get().getAwsEndpoint().isPresent()) {
      elbClient.setEndpoint(configuration.get().getAwsEndpoint().get());
    }
    if (configuration.isPresent() && configuration.get().getAwsRegion().isPresent()) {
      elbClient.configureRegion(Regions.fromName(configuration.get().getAwsRegion().get()));
    }

    return elbClient;
  }

  @Singleton
  @Provides
  public CuratorFramework provideCurator(ZooKeeperConfiguration config, BaragonConnectionStateListener connectionStateListener) {
    CuratorFramework client = CuratorFrameworkFactory.builder()
      .connectString(config.getQuorum())
      .sessionTimeoutMs(config.getSessionTimeoutMillis())
      .connectionTimeoutMs(config.getConnectTimeoutMillis())
      .retryPolicy(new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()))
      .defaultData(new byte[0])
      .build();

    client.getConnectionStateListenable().addListener(connectionStateListener);

    client.start();

    return client.usingNamespace(config.getZkNamespace());
  }

  @Provides
  @Singleton
  public Optional<SentryConfiguration> sentryConfiguration(final BaragonConfiguration config) {
    return config.getSentryConfiguration();
  }
}

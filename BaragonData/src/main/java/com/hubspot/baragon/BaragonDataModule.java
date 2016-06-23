package com.hubspot.baragon;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.migrations.UpstreamsMigration;
import com.hubspot.baragon.migrations.ZkDataMigration;
import com.hubspot.baragon.models.BaragonAuthKey;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;


public class BaragonDataModule extends AbstractModule {
  public static final String BARAGON_AGENT_REQUEST_URI_FORMAT = "baragon.agent.request.uri.format";
  public static final String BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT = "baragon.agent.batch.request.uri.format";
  public static final String BARAGON_AGENT_MAX_ATTEMPTS = "baragon.agent.maxAttempts";
  public static final String BARAGON_AGENT_REQUEST_TIMEOUT_MS = "baragon.agent.requestTimeoutMs";

  public static final String BARAGON_SERVICE_WORKER_LAST_START = "baragon.service.worker.lastStartedAt";
  public static final String BARAGON_ELB_WORKER_LAST_START = "baragon.service.elb.lastStartedAt";

  public static final String BARAGON_AUTH_KEY_MAP = "baragon.auth.keyMap";

  public static final String BARAGON_AUTH_KEY = "baragon.auth.key";
  public static final String BARAGON_AUTH_PATH_CACHE = "baragon.auth.pathCache";

  public static final String BARAGON_ZK_CONNECTION_STATE = "baragon.zk.connectionState";

  public static final String BARAGON_SERVICE_LEADER_LATCH = "baragon.service.leaderLatch";



  @Override
  protected void configure() {
    Multibinder<ZkDataMigration> zkMigrationBinder = Multibinder.newSetBinder(binder(), ZkDataMigration.class);
    zkMigrationBinder.addBinding().to(UpstreamsMigration.class);
  }

  @Singleton
  @Provides
  public ObjectMapper provideObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());

    return objectMapper;
  }

  @Provides
  @Singleton
  public Random providesRandom() {
    return new Random();
  }

  @Provides
  @Singleton
  @Named(BARAGON_ZK_CONNECTION_STATE)
  public AtomicReference<ConnectionState> providesConnectionState() {
    return new AtomicReference<>();
  }

  @Provides
  @Singleton
  @Named(BARAGON_AUTH_KEY_MAP)
  public AtomicReference<Map<String, BaragonAuthKey>> providesBaragonAuthKeyMap(BaragonAuthDatastore datastore) {
    return new AtomicReference<>(datastore.getAuthKeyMap());
  }

  @Provides
  @Singleton
  @Named(BARAGON_AUTH_PATH_CACHE)
  public PathChildrenCache providesAuthPathChildrenCache(CuratorFramework curatorFramework) {
    return new PathChildrenCache(curatorFramework, BaragonAuthDatastore.AUTH_KEYS_PATH, false);
  }

  @Provides
  @Named(BARAGON_AUTH_KEY)
  public Optional<String> providesBaragonAuthKey(AuthConfiguration authConfiguration) {
    return authConfiguration.getKey();
  }

  @Provides
  @Singleton
  public MetricRegistry provideRegistry(Environment environment) {
    return environment.metrics();
  }
}

package com.hubspot.baragon.service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;


public class BaragonServiceModule extends AbstractModule {
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";
  public static final String BARAGON_SERVICE_LEADER_LATCH = "baragon.service.leaderLatch";
  public static final String BARAGON_SERVICE_WORKER_INTERVAL_MS = "baragon.service.worker.intervalMs";
  public static final String BARAGON_SERVICE_WORKER_LAST_START = "baragon.service.worker.lastStartedAt";

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
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
  @Named(BaragonBaseModule.BARAGON_AGENT_REQUEST_URI_FORMAT)
  public String provideAgentUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentRequestUriFormat();
  }

  @Provides
  @Named(BaragonBaseModule.BARAGON_AGENT_MAX_ATTEMPTS)
  public Integer provideAgentMaxAttempts(BaragonConfiguration configuration) {
    return configuration.getAgentMaxAttempts();
  }

  @Provides
  @Named(BARAGON_SERVICE_WORKER_INTERVAL_MS)
  public long provideWorkerIntervalMs(BaragonConfiguration configuration) {
    return configuration.getWorkerIntervalMs();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_LEADER_LATCH)
  public LeaderLatch providesWorkerLeaderLatch(CuratorFramework curatorFramework) {
    return new LeaderLatch(curatorFramework, "/singularity/workers");
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(1);
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_WORKER_LAST_START)
  public AtomicLong providesWorkerLastStartAt() {
    return new AtomicLong();
  }
}
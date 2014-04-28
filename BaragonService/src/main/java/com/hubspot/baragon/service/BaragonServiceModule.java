package com.hubspot.baragon.service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.HttpClientConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class BaragonServiceModule extends AbstractModule {
  public static final String BARAGON_SERVICE_HTTP_CLIENT = "baragon.service.http.client";
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";
  public static final String BARAGON_SERVICE_LEADER_LATCH = "baragon.service.leaderLatch";
  public static final String BARAGON_AGENT_REQUEST_URI_FORMAT = "baragon.agent.request.uri.format";

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
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
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }

  @Provides
  public HttpClientConfiguration provideHttpClientConfiguration(BaragonConfiguration configuration) {
    return configuration.getHttpClientConfiguration();
  }

  @Provides
  @Named(BARAGON_AGENT_REQUEST_URI_FORMAT)
  public String provideAgentUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentRequestUriFormat();
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
}
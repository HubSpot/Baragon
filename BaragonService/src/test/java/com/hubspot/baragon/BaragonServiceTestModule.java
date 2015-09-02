package com.hubspot.baragon;

import java.util.concurrent.atomic.AtomicLong;

import com.hubspot.baragon.service.BaragonLoadBalancerTestDatastore;
import com.hubspot.baragon.service.BaragonServiceModule;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class BaragonServiceTestModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(TestingServer.class).in(Scopes.SINGLETON);
    bind(BaragonLoadBalancerDatastore.class).to(BaragonLoadBalancerTestDatastore.class).in(Scopes.SINGLETON);
  }

  @Singleton
  @Provides
  public CuratorFramework provideCurator(TestingServer testingServer) throws InterruptedException {
    final CuratorFramework client = CuratorFrameworkFactory.newClient(testingServer.getConnectString(), new RetryOneTime(1));
    client.start();
    return client;
  }

  @Singleton
  @Provides
  @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START)
  public AtomicLong providesLastStart() {
    return new AtomicLong();
  }

  @Provides
  @Singleton
  @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT)
  public AsyncHttpClient providesHttpClient(HttpClientConfiguration config) {
    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

    builder.setMaxRequestRetry(config.getMaxRequestRetry());
    builder.setRequestTimeoutInMs(config.getRequestTimeoutInMs());
    builder.setFollowRedirects(true);
    builder.setConnectionTimeoutInMs(config.getConnectionTimeoutInMs());
    builder.setUserAgent(config.getUserAgent());

    return new AsyncHttpClient(builder.build());
  }

  @Singleton
  @Provides
  public ObjectMapper provideObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());

    return objectMapper;
  }
}
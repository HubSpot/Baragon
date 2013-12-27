package com.hubspot.baragon;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.healthchecks.HealthCheckClient;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LocalLbAdapter;
import com.hubspot.baragon.webhooks.WebhookClient;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class BaragonBaseModule extends AbstractModule {
  public static final String LB_PROXY_TEMPLATE = "baragon.proxy.template";
  public static final String LB_UPSTREAM_TEMPLATE = "baragon.upstream.template";

  public static final String BARAGON_USER_AGENT = "Baragon/0.1 (+https://git.hubteam.com/HubSpot/Baragon)";

  @Override
  protected void configure() {
    // load balancer adapters
    bind(Key.get(LbAdapter.class, Names.named("nginx"))).to(LocalLbAdapter.class);
  }

  @Singleton
  @Provides
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) {
    CuratorFramework client = CuratorFrameworkFactory.newClient(
        config.getQuorum(),
        config.getSessionTimeoutMillis(),
        config.getConnectTimeoutMillis(),
        new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()));

    client.start();

    return client.usingNamespace(config.getZkNamespace());
  }
  
  @Provides
  @Singleton
  @HealthCheckClient
  public AsyncHttpClient providesHealthCheckClient() {
    return new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
      .setConnectionTimeoutInMs(5000)
      .setRequestTimeoutInMs(5000)
      .setFollowRedirects(true)
      .setMaxRequestRetry(0)
      .setUserAgent(BARAGON_USER_AGENT)
      .build());
  }

  @Provides
  @Singleton
  @WebhookClient
  public AsyncHttpClient providesWebhookClient() {
    return new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
        .setConnectionTimeoutInMs(5000)
        .setRequestTimeoutInMs(5000)
        .setFollowRedirects(true)
        .setMaxRequestRetry(3)
        .setUserAgent(BARAGON_USER_AGENT)
        .build());
  }


  @Provides
  @Singleton
  public AsyncHttpClient providesAsyncHttpClient() {
    return new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
        .setFollowRedirects(true)
        .setRequestTimeoutInMs(10000)
        .build());
  }
}

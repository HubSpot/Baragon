package com.hubspot.baragon.config;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.baragon.healthchecks.HealthCheckClient;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.nginx.NginxAdapter;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;

import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaragonBaseModule extends AbstractModule {
  public static final String BARAGON_USER_AGENT = "Baragon/0.1 (+https://git.hubteam.com/HubSpot/Baragon)";

  @Override
  protected void configure() {
    // load balancer adapters
    bind(Key.get(LbAdapter.class, Names.named("nginx"))).to(NginxAdapter.class);

    // config generation
    final Pattern templatePattern = Pattern.compile("baragon-(.*)\\.mustache");
    final MustacheFactory mustache = new DefaultMustacheFactory();
    for (String filename : new Reflections(ClasspathHelper.forPackage("com.hubspot.baragon"), new ResourcesScanner()).getResources(templatePattern)) {
      final Matcher matcher = templatePattern.matcher(filename);
      if (matcher.matches()) {
        bind(Mustache.class).annotatedWith(Names.named(matcher.group(1))).toInstance(mustache.compile(filename));
      }
    }
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
  public AsyncHttpClient providesAsyncHttpClient() {
    return new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
        .setFollowRedirects(true)
        .setRequestTimeoutInMs(10000)
        .build());
  }
}

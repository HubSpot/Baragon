package com.hubspot.baragon.watcher;

import com.google.common.base.Supplier;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.ringleader.watcher.PersistentWatcher;
import com.hubspot.ringleader.watcher.WatcherFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class BaragonWatcherModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BaragonStateWatcher.class).asEagerSingleton();

    Multibinder.newSetBinder(binder(), BaragonStateListener.class);
  }

  @Baragon
  @Provides
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) {
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(config.getQuorum())
        .sessionTimeoutMs(config.getSessionTimeoutMillis())
        .connectionTimeoutMs(config.getConnectTimeoutMillis())
        .retryPolicy(new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()))
        .namespace(config.getZkNamespace())
        .build();

    client.start();

    return client;
  }

  @Baragon
  @Provides
  @Singleton
  public PersistentWatcher provideWatcher(@Baragon final Provider<CuratorFramework> curatorProvider) {
    return new WatcherFactory(new Supplier<CuratorFramework>() {

      @Override
      public CuratorFramework get() {
        return curatorProvider.get();
      }
    }).dataWatcher("/state-last-updated");
  }
}

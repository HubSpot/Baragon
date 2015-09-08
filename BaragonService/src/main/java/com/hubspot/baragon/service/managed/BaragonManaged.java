package com.hubspot.baragon.service.managed;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.listeners.AbstractLatchListener;
import io.dropwizard.lifecycle.Managed;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BaragonManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonManaged.class);

  private final ScheduledExecutorService executorService;
  private final LeaderLatch leaderLatch;
  private final BaragonConfiguration config;
  private final BaragonAuthDatastore authDatastore;
  private final Set<AbstractLatchListener> listeners;

  @Inject
  public BaragonManaged(Set<AbstractLatchListener> listeners,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                        @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                        BaragonConfiguration config,
                        BaragonAuthDatastore authDatastore) {
    this.listeners = listeners;
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.config = config;
    this.authDatastore = authDatastore;
  }

  @Override
  public void start() throws Exception {
    if (config.getAuthConfiguration().getKey().isPresent() && config.getAuthConfiguration().isEnabled()) {
      if (!authDatastore.getAuthKeyInfo(config.getAuthConfiguration().getKey().get()).isPresent()) {
        authDatastore.addAuthKey(new BaragonAuthKey(config.getAuthConfiguration().getKey().get(), "baragon", System.currentTimeMillis(), Optional.<Long>absent()));
      }
    }
    for (AbstractLatchListener listener : listeners) {
      if (listener.isEnabled()) {
        leaderLatch.addListener(listener);
      }
    }
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
  }
}

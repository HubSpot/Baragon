package com.hubspot.baragon.service.listeners;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.worker.BaragonElbSyncWorker;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

public class ElbSyncWorkerListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final ScheduledExecutorService executorService;
  private final BaragonElbSyncWorker elbWorker;
  private final BaragonConfiguration config;

  private ScheduledFuture<?> elbWorkerFuture = null;

  @Inject
  public ElbSyncWorkerListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                               BaragonConfiguration config,
                               BaragonElbSyncWorker elbWorker) {
    this.executorService = executorService;
    this.config = config;
    this.elbWorker = elbWorker;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting ElbSyncWorker...");

    if (elbWorkerFuture != null) {
      elbWorkerFuture.cancel(false);
    }

    elbWorkerFuture = executorService.scheduleAtFixedRate(elbWorker, config.getElbConfiguration().getInitialDelaySeconds(), config.getElbConfiguration().getIntervalSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    elbWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return (config.getElbConfiguration() != null && config.getElbConfiguration().isEnabled());
  }
}
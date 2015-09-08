package com.hubspot.baragon.service.listeners;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.worker.BaragonElbSyncWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElbSyncWorkerListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final ScheduledExecutorService executorService;
  private final BaragonElbSyncWorker elbWorker;
  private final Optional<ElbConfiguration> config;

  private ScheduledFuture<?> elbWorkerFuture = null;

  @Inject
  public ElbSyncWorkerListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                               Optional<ElbConfiguration> config,
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

    elbWorkerFuture = executorService.scheduleAtFixedRate(elbWorker, config.get().getInitialDelaySeconds(), config.get().getIntervalSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    elbWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return (config.isPresent() && config.get().isEnabled());
  }
}

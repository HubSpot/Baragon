package com.hubspot.baragon.service.listeners;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.worker.BaragonBackgroundStateUpdatingWorker;

public class BackgroundStateUpdatingListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final BaragonConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final BaragonBackgroundStateUpdatingWorker backgroundStateUpdatingWorker;
  private ScheduledFuture<?> backgroundStateUpdaterFuture = null;

  @Inject
  public BackgroundStateUpdatingListener(BaragonBackgroundStateUpdatingWorker backgroundStateUpdatingWorker, BaragonConfiguration configuration, @Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService) {
    this.configuration = configuration;
    this.executorService = executorService;
    this.backgroundStateUpdatingWorker = backgroundStateUpdatingWorker;
  }

  @Override
  public boolean isEnabled() {
    return configuration.isUpdateStateInBackground();
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting BaragonBackgroundStateUpdatingWorker...");

    if (backgroundStateUpdaterFuture != null) {
      backgroundStateUpdaterFuture.cancel(false);
    }

    backgroundStateUpdaterFuture = executorService.scheduleAtFixedRate(backgroundStateUpdatingWorker, configuration.getBackgroundStateUpdateIntervalMs(), configuration.getBackgroundStateUpdateIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader! Stopping BaragonBackgroundStateUpdatingWorker...");

    if (backgroundStateUpdaterFuture != null) {
      backgroundStateUpdaterFuture.cancel(false);
    }
  }
}

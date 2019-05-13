package com.hubspot.baragon.service.listeners;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.worker.BaragonStatePersisterWorker;

public class StatePersisterWorkerListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(StatePersisterWorkerListener.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private final BaragonConfiguration baragonConfiguration;
  private final BaragonStatePersisterWorker baragonStatePersisterWorker;

  private ScheduledFuture<?> statePersisterWorkerFuture = null;

  public StatePersisterWorkerListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService scheduledExecutorService,
                                      BaragonConfiguration baragonConfiguration,
                                      BaragonStatePersisterWorker baragonStatePersisterWorker) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.baragonConfiguration = baragonConfiguration;
    this.baragonStatePersisterWorker = baragonStatePersisterWorker;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting BaragonStatePersisterWorker...");

    if (statePersisterWorkerFuture != null) {
      statePersisterWorkerFuture.cancel(false);
    }
    statePersisterWorkerFuture = scheduledExecutorService.scheduleAtFixedRate(
        baragonStatePersisterWorker,
        baragonConfiguration.getWorkerConfiguration().getInitialDelayMs(),
        baragonConfiguration.getWorkerConfiguration().getIntervalMs(),
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are NOT the leader!");
    statePersisterWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return baragonConfiguration.getWorkerConfiguration().isEnabled();
  }
}

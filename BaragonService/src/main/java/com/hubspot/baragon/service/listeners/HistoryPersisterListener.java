package com.hubspot.baragon.service.listeners;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.worker.HistoryPersisterWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryPersisterListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryPersisterListener.class);

  private static final int PERSIST_HISTORY_EVERY_SECS = 120;

  private final ScheduledExecutorService executorService;
  private final BaragonConfiguration configuration;
  private final HistoryPersisterWorker historyPersisterWorker;

  private ScheduledFuture<?> historyPersisterWorkerFuture = null;

  @Inject
  public HistoryPersisterListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                                  BaragonConfiguration configuration,
                                  HistoryPersisterWorker historyPersisterWorker) {
    this.executorService = executorService;
    this.configuration = configuration;
    this.historyPersisterWorker = historyPersisterWorker;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting ElbSyncWorker...");

    if (historyPersisterWorkerFuture != null) {
      historyPersisterWorkerFuture.cancel(false);
    }

    historyPersisterWorkerFuture = executorService.scheduleAtFixedRate(historyPersisterWorker, PERSIST_HISTORY_EVERY_SECS, PERSIST_HISTORY_EVERY_SECS, TimeUnit.SECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    historyPersisterWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return configuration.getDatabaseConfiguration().isPresent();
  }
}

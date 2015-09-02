package com.hubspot.baragon.service.listeners;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

public class RequestWorkerListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(RequestWorkerListener.class);

  private final ScheduledExecutorService executorService;
  private final BaragonRequestWorker requestWorker;
  private final BaragonConfiguration config;

  private ScheduledFuture<?> requestWorkerFuture = null;

  @Inject
  public RequestWorkerListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                                BaragonConfiguration config,
                                BaragonRequestWorker requestWorker) {
    this.executorService = executorService;
    this.config = config;
    this.requestWorker = requestWorker;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting RequestWorker...");

    if (requestWorkerFuture != null) {
      requestWorkerFuture.cancel(false);
    }

    requestWorkerFuture = executorService.scheduleAtFixedRate(requestWorker, config.getWorkerConfiguration().getInitialDelayMs(), config.getWorkerConfiguration().getIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    requestWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return config.getWorkerConfiguration().isEnabled();
  }
}

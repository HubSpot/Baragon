package com.hubspot.baragon.service.listeners;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.worker.RequestPurgerWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestPurgerListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(RequestPurgerListener.class);

  private final ScheduledExecutorService executorService;
  private final BaragonConfiguration configuration;
  private final RequestPurgerWorker requestPurgerWorker;

  private ScheduledFuture<?> requestPurgerWorkerFuture = null;

  @Inject
  public RequestPurgerListener(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                               BaragonConfiguration configuration,
                               RequestPurgerWorker requestPurgerWorker) {
    this.executorService = executorService;
    this.configuration = configuration;
    this.requestPurgerWorker = requestPurgerWorker;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting Old Request Purger...");

    if (requestPurgerWorkerFuture != null) {
      requestPurgerWorkerFuture.cancel(false);
    }

    requestPurgerWorkerFuture = executorService.scheduleAtFixedRate(
      requestPurgerWorker,
      configuration.getHistoryConfiguration().getPurgeEveryHours(),
      configuration.getHistoryConfiguration().getPurgeEveryHours(),
      TimeUnit.HOURS
    );
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    requestPurgerWorkerFuture.cancel(false);
  }

  @Override
  public boolean isEnabled() {
    return configuration.getHistoryConfiguration().isPurgeOldRequests();
  }
}

package com.hubspot.baragon.agent;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.poller.PollerRunnable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class BaragonAgentScheduler {
  private static final Log LOG = LogFactory.getLog(BaragonAgentScheduler.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private final int pollInterval;
  private final PollerRunnable pollerRunnable;

  private ScheduledFuture<?> scheduledFuture = null;

  @Inject
  public BaragonAgentScheduler(ScheduledExecutorService scheduledExecutorService,
                               @Named(BaragonAgentServiceModule.UPSTREAM_POLL_INTERVAL_PROPERTY) int pollInterval,
                               PollerRunnable pollerRunnable) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.pollInterval = pollInterval;
    this.pollerRunnable = pollerRunnable;
  }

  public synchronized void start() {
    if (scheduledFuture == null || scheduledFuture.isDone()) {
      LOG.info(String.format("Starting upstream poller (%sms interval)...", pollInterval));
      scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(pollerRunnable, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
    } else {
      LOG.warn("BaragonAgentScheduler was already started!");
    }
  }

  public synchronized void stop() {
    try {
      LOG.info("Stopping upstream poller...");
      scheduledFuture.cancel(false);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void close() throws InterruptedException {
    LOG.info("Shutting down scheduled executor service");
    scheduledExecutorService.shutdownNow();
    scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
  }
}

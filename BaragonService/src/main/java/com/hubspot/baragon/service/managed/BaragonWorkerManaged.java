package com.hubspot.baragon.service.managed;

import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.worker.BaragonRequestWorker;

public class BaragonWorkerManaged implements Managed, LeaderLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonWorkerManaged.class);

  private final ScheduledExecutorService executorService;
  private final BaragonRequestWorker worker;
  private final LeaderLatch leaderLatch;
  private final BaragonConfiguration config;

  private ScheduledFuture<?> workerFuture = null;

  @Inject
  public BaragonWorkerManaged(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                              @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                              BaragonConfiguration config,
                              BaragonRequestWorker worker) {
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.config = config;
    this.worker = worker;
  }

  @Override
  public void start() throws Exception {
    if (config.getWorkerConfiguration().isEnabled()) {
      if (config.getWorkerIntervalMs().isPresent()) {
        LOG.warn("workerIntervalMs configuration setting is deprecated! Use worker.intervalMs instead.");
      }

      leaderLatch.addListener(this);
      leaderLatch.start();
    }
  }

  @Override
  public void stop() throws Exception {
    if (config.getWorkerConfiguration().isEnabled()) {
      leaderLatch.close();
      executorService.shutdown();
    }
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader!");

    if (workerFuture != null) {
      workerFuture.cancel(false);
    }

    workerFuture = executorService.scheduleAtFixedRate(worker, config.getWorkerConfiguration().getInitialDelayMs(), config.getWorkerConfiguration().getIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    workerFuture.cancel(false);
  }
}

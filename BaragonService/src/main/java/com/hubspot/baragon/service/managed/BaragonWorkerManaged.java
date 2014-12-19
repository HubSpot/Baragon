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
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.worker.BaragonRequestWorker;

public class BaragonWorkerManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonWorkerManaged.class);

  private final ScheduledExecutorService executorService;
  private final BaragonRequestWorker worker;
  private final LeaderLatch leaderLatch;
  private final long workerIntervalMs;

  @Inject
  public BaragonWorkerManaged(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_WORKER_INTERVAL_MS) long workerIntervalMs,
                              BaragonRequestWorker worker) {
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.workerIntervalMs = workerIntervalMs;
    this.worker = worker;
  }

  @Override
  public void start() throws Exception {
    leaderLatch.addListener(new LeaderLatchListener() {
      private ScheduledFuture<?> future = null;

      @Override
      public void isLeader() {
        LOG.info("We are the leader!");

        if (future != null) {
          future.cancel(false);
        }

        future = executorService.scheduleAtFixedRate(worker, 0, workerIntervalMs, TimeUnit.MILLISECONDS);
      }

      @Override
      public void notLeader() {
        LOG.info("We are not the leader!");
        future.cancel(false);
      }
    });

    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
  }
}

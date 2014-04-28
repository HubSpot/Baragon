package com.hubspot.baragon.service.managed;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.worker.PendingRequestWorker;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaragonWorkerManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(BaragonWorkerManaged.class);

  private final ScheduledExecutorService executorService;
  private final PendingRequestWorker worker;
  private final LeaderLatch leaderLatch;

  @Inject
  public BaragonWorkerManaged(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                              PendingRequestWorker worker) {
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.worker = worker;
  }

  @Override
  public void start() throws Exception {
    leaderLatch.addListener(new LeaderLatchListener() {
      @Override
      public void isLeader() {
        LOG.info("We are the leader!");
      }

      @Override
      public void notLeader() {
        LOG.info("We are not the leader!");
      }
    });

    leaderLatch.start();
    executorService.scheduleAtFixedRate(worker, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
  }
}

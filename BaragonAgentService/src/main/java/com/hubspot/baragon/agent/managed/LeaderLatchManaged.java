package com.hubspot.baragon.agent.managed;

import com.hubspot.baragon.agent.BaragonAgentScheduler;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

public class LeaderLatchManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(LeaderLatchManaged.class);
  
  private final LeaderLatch leaderLatch;
  private final BaragonAgentScheduler upstreamPoller;
  
  @Inject
  public LeaderLatchManaged(LeaderLatch leaderLatch, BaragonAgentScheduler upstreamPoller) {
    this.leaderLatch = leaderLatch;
    this.upstreamPoller = upstreamPoller;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting leader latch");
    leaderLatch.addListener(new LeaderLatchListener() {
      @Override
      public void isLeader() {
        LOG.info("Elected leader!");
        upstreamPoller.start();
      }

      @Override
      public void notLeader() {
        LOG.info("Not leader.");
        upstreamPoller.stop();
      }
    });
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    LOG.info("Closing leader latch");
    leaderLatch.close();
    upstreamPoller.close();
  }
}

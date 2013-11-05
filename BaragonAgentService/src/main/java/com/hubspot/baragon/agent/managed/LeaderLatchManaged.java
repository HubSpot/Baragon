package com.hubspot.baragon.agent.managed;

import io.dropwizard.lifecycle.Managed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

public class LeaderLatchManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(LeaderLatchManaged.class);
  
  private final LeaderLatch leaderLatch;
  
  @Inject
  public LeaderLatchManaged(LeaderLatch leaderLatch) {
    this.leaderLatch = leaderLatch;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting leader latch");
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    LOG.info("Closing leader latch");
    leaderLatch.close();
  }

}

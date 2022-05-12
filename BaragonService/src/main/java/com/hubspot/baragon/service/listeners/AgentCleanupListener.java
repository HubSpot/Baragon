package com.hubspot.baragon.service.listeners;

import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.service.BaragonServiceModule;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentCleanupListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final ScheduledExecutorService executorService;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  private ScheduledFuture<?> checkFuture = null;

  @Inject
  public AgentCleanupListener(
    @Named(
      BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR
    ) ScheduledExecutorService executorService,
    BaragonKnownAgentsDatastore knownAgentsDatastore,
    BaragonLoadBalancerDatastore loadBalancerDatastore
  ) {
    this.executorService = executorService;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting ElbSyncWorker...");

    if (checkFuture != null) {
      checkFuture.cancel(true);
    }

    checkFuture =
      executorService.scheduleAtFixedRate(
        this::cleanOldKnownAgents,
        60,
        60,
        TimeUnit.SECONDS
      );
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    checkFuture.cancel(true);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  private void cleanOldKnownAgents() {
    try {
      long now = System.currentTimeMillis();
      long threshold = now - TimeUnit.MINUTES.toMillis(30);
      for (String group : loadBalancerDatastore.getLoadBalancerGroupNames()) {
        knownAgentsDatastore
          .getKnownAgentsMetadata(group)
          .forEach(
            k -> {
              if (k.getLastSeenAt() < threshold) {
                knownAgentsDatastore.removeKnownAgent(group, k.getAgentId());
              }
            }
          );
      }
    } catch (Exception e) {
      LOG.error("Could not clean up old known agents", e);
    }
  }
}

package com.hubspot.baragon.agent.workers;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;

public class AgentHeartbeatWorker implements Runnable {
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonAgentMetadata baragonAgentMetadata;
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public AgentHeartbeatWorker(BaragonKnownAgentsDatastore knownAgentsDatastore,
                              BaragonAgentMetadata baragonAgentMetadata,
                              LoadBalancerConfiguration loadBalancerConfiguration) {
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.baragonAgentMetadata = baragonAgentMetadata;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  @Override
  public void run() {
    knownAgentsDatastore.updateKnownAgentLastSeenAt(loadBalancerConfiguration.getName(), baragonAgentMetadata.getAgentId(), System.currentTimeMillis());
  }
}

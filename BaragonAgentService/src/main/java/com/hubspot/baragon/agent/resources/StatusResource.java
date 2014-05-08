package com.hubspot.baragon.agent.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.baragon.agent.models.AgentStatus;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.atomic.AtomicReference;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final LocalLbAdapter adapter;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final LeaderLatch leaderLatch;
  private final AtomicReference<String> mostRecentRequestId;

  @Inject
  public StatusResource(LocalLbAdapter adapter, LoadBalancerConfiguration loadBalancerConfiguration,
                        @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId) {
    this.adapter = adapter;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.leaderLatch = leaderLatch;
    this.mostRecentRequestId = mostRecentRequestId;
  }

  @GET
  public AgentStatus getStatus() {
    boolean validConfigs = true;
    Optional<String> errorMessage = Optional.absent();

    try {
      adapter.checkConfigs();
    } catch (InvalidConfigException e) {
      validConfigs = false;
      errorMessage = Optional.of(e.getMessage());
    }

    return new AgentStatus(loadBalancerConfiguration.getName(), validConfigs, errorMessage, leaderLatch.hasLeadership(), mostRecentRequestId.get());
  }
}

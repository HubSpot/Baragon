package com.hubspot.baragon.agent.resources;

import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonAgentStatus;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final LocalLbAdapter adapter;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final LeaderLatch leaderLatch;
  private final AtomicReference<String> mostRecentRequestId;
  private final AtomicReference<ConnectionState> connectionState;
  private final BaragonAgentMetadata agentMetadata;
  private final AtomicReference<Boolean> validConfigs;
  private final AtomicReference<Optional<String>> errorMessage;

  @Inject
  public StatusResource(LocalLbAdapter adapter,
                        LoadBalancerConfiguration loadBalancerConfiguration,
                        BaragonAgentMetadata agentMetadata,
                        @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                        @Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState,
                        @Named(BaragonAgentServiceModule.VALID_CONFIGS) AtomicReference<Boolean> validConfigs,
                        @Named(BaragonAgentServiceModule.CONFIG_ERROR_MESSAGE) AtomicReference<Optional<String>> errorMessage) {
    this.adapter = adapter;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.leaderLatch = leaderLatch;
    this.mostRecentRequestId = mostRecentRequestId;
    this.connectionState = connectionState;
    this.agentMetadata = agentMetadata;
    this.validConfigs = validConfigs;
    this.errorMessage = errorMessage;
  }

  @GET
  @NoAuth
  public BaragonAgentStatus getStatus(@DefaultValue("false") @QueryParam("skipCache") boolean skipCache) {
    if (skipCache) {
      try {
        adapter.checkConfigs();
        validConfigs.set(true);
        errorMessage.set(Optional.<String>absent());
      } catch (InvalidConfigException e) {
        validConfigs.set(false);
        errorMessage.set(Optional.of(e.getMessage()));
      }
    }

    final ConnectionState currentConnectionState = connectionState.get();

    final String connectionStateString = currentConnectionState == null ? "UNKNOWN" : currentConnectionState.name();

    return new BaragonAgentStatus(loadBalancerConfiguration.getName(), validConfigs.get(), errorMessage.get(), leaderLatch.hasLeadership(), mostRecentRequestId.get(), connectionStateString, agentMetadata);
  }
}

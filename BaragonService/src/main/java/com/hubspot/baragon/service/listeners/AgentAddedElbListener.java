package com.hubspot.baragon.service.listeners;

import java.util.Arrays;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.ElbConfiguration;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentAddedElbListener extends AbstractAgentWatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(AgentAddedElbListener.class);

  private final Optional<ElbConfiguration> config;
  private final AmazonElasticLoadBalancingClient elbClient;

  @Inject
  public AgentAddedElbListener(CuratorFramework curatorFramework,
                               BaragonLoadBalancerDatastore loadBalancerDatastore,
                               Optional<ElbConfiguration> config,
                               @Named(BaragonDataModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient) {
    super(curatorFramework, loadBalancerDatastore);
    this.config = config;
    this.elbClient = elbClient;
  }

  @Override
  public boolean isEnabled() {
    return (config.isPresent() && config.get().isEnabled());
  }

  @Override
  public void agentUpdated(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) {
    if (group.isPresent()) {
      if (!group.get().getSources().isEmpty()) {
        if (agent.getInstanceId().isPresent()) {
          LOG.debug(String.format("Trying to register agent %s with ELB", agent.getAgentId()));
          for (String elbName : group.get().getSources()) {
            Instance instance = new Instance(agent.getInstanceId().get());
            RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance));
            try {
              elbClient.registerInstancesWithLoadBalancer(request);
              LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
            } catch (AmazonClientException e) {
              LOG.error("Could not register %s with elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
            }
          }
        } else {
         LOG.debug(String.format("No instance id for agent %s, can't add to ELB", agent.getAgentId()));
        }
      } else {
        LOG.debug(String.format("No traffic sources for group %s, not adding agent %s to an ELB", group.get().getName(), agent.getAgentId()));
      }
    } else {
      LOG.debug(String.format("Group %s not found for agent %s", groupName, agent.getAgentId()));
    }
  }

  @Override
  public void agentRemoved(PathChildrenCacheEvent event) {}
}

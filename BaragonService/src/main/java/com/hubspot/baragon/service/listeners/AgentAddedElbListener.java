package com.hubspot.baragon.service.listeners;

import java.util.Arrays;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
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
    try {
      if (shouldAddInstance(agent, group, groupName)) {
        LOG.debug(String.format("Trying to register agent %s with ELB", agent.getAgentId()));
        for (String elbName : group.get().getSources()) {
          Instance instance = new Instance(agent.getEc2().getInstanceId().get());
          RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance));
          try {
            elbClient.registerInstancesWithLoadBalancer(request);
            LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
          } catch (AmazonClientException e) {
            LOG.error("Could not register %s with elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
          }
        }
      }
    } catch (Exception e) {
      LOG.error(String.format("Could not check instance elb registration due to error %s", e), e);
    }
  }

  private LoadBalancerDescription elbByName(String elbName) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(Arrays.asList(elbName));
    return elbClient.describeLoadBalancers(request).getLoadBalancerDescriptions().get(0);
  }

  private void checkAZEnabled(BaragonAgentMetadata agent, String elbName, LoadBalancerDescription elb) {
    if (agent.getEc2().getAvailabilityZone().isPresent()) {
      String availabilityZone = agent.getEc2().getAvailabilityZone().get();
      if (elb.getLoadBalancerName().equals(elbName) && elb.getAvailabilityZones().contains(availabilityZone)) {
        try {
          if (agent.getEc2().getSubnetId().isPresent()) {
            addSubnet(agent, elb);
          } else {
            enabledAZ(agent, availabilityZone, elb);
          }
        } catch (AmazonClientException e) {
          LOG.error("Could not enable availability zone %s for elb %s due to error", availabilityZone, elb.getLoadBalancerName(), e);
        }
      }
    } else {
      LOG.warn(String.format("No availability zone specified for agent %s", agent.getAgentId()));
    }
  }

  private void addSubnet(BaragonAgentMetadata agent, LoadBalancerDescription elb) {
    AttachLoadBalancerToSubnetsRequest request = new AttachLoadBalancerToSubnetsRequest();
    request.setLoadBalancerName(elb.getLoadBalancerName());
    List<String> subnets = elb.getSubnets();
    subnets.add(agent.getEc2().getSubnetId().get());
    request.setSubnets(subnets);
    elbClient.attachLoadBalancerToSubnets(request);
  }

  private void enabledAZ(BaragonAgentMetadata agent, String availabilityZone, LoadBalancerDescription elb) {
    LOG.info(String.format("Enabling availability zone %s in preparation for agent %s", availabilityZone, agent.getAgentId()));
    List<String> availabilityZones = elb.getAvailabilityZones();
    availabilityZones.add(availabilityZone);
    EnableAvailabilityZonesForLoadBalancerRequest request = new EnableAvailabilityZonesForLoadBalancerRequest();
    request.setAvailabilityZones(availabilityZones);
    request.setLoadBalancerName(elb.getLoadBalancerName());
    elbClient.enableAvailabilityZonesForLoadBalancer(request);
  }

  private boolean shouldAddInstance(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) {
    if (group.isPresent()) {
      if (!group.get().getSources().isEmpty()) {
        if (agent.getEc2().getInstanceId().isPresent()) {
          for (String elbName : group.get().getSources()) {
            Instance instance = new Instance(agent.getEc2().getInstanceId().get());
            LoadBalancerDescription elb = elbByName(elbName);
            if (!elb.getInstances().contains(instance)) {
              checkAZEnabled(agent, elbName, elb);
              return true;
            } else {
              LOG.debug(String.format("Agent %s already registered with ELB %s", agent.getAgentId(), elbName));
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
    return false;
  }

  @Override
  public void agentRemoved(PathChildrenCacheEvent event) {}
}

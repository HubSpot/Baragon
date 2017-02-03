package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.TrafficSource;
import com.hubspot.baragon.models.TrafficSourceType;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.elb.ApplicationLoadBalancer;
import com.hubspot.baragon.service.elb.ClassicLoadBalancer;
import com.hubspot.baragon.service.elb.ElasticLoadBalancer;
import com.hubspot.baragon.service.elb.RegisterInstanceResult;
import com.hubspot.baragon.service.exceptions.NoMatchingElbForVpcException;

@Singleton
public class ElbManager {
  private static final Logger LOG = LoggerFactory.getLogger(ElbManager.class);

  private final ApplicationLoadBalancer applicationLoadBalancer;
  private final ClassicLoadBalancer classicLoadBalancer;

  private final Optional<ElbConfiguration> configuration;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public ElbManager(ApplicationLoadBalancer applicationLoadBalancer,
                    ClassicLoadBalancer classicLoadBalancer,
                    BaragonLoadBalancerDatastore loadBalancerDatastore,
                    Optional<ElbConfiguration> configuration) {
    this.applicationLoadBalancer = applicationLoadBalancer;
    this.classicLoadBalancer = classicLoadBalancer;

    this.configuration = configuration;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  public boolean isElbConfigured() {
    return (configuration.isPresent() && configuration.get().isEnabled());
  }

  public boolean isActiveAndHealthy(Optional<BaragonGroup> group, BaragonAgentMetadata agent) {
    for (TrafficSource source : group.get().getTrafficSources()) {
      if (getLoadBalancer(source.getType()).isInstanceHealthy(agent.getEc2().getInstanceId().get(), source.getName())) {
        return true;
      }
    }
    return false;
  }

  public void attemptRemoveAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) throws AmazonClientException {
    if (isElbEnabledAgent(agent, group, groupName)) {
      for (TrafficSource source : group.get().getTrafficSources()) {
        Instance instance = new Instance(agent.getEc2().getInstanceId().get());
        getLoadBalancer(source.getType()).removeInstance(instance, source.getName(), agent.getAgentId());
      }
    }
  }

  public void attemptAddAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) throws AmazonClientException, NoMatchingElbForVpcException {
    if (isElbEnabledAgent(agent, group, groupName)) {
      List<RegisterInstanceResult> results = new ArrayList<>();
      for (TrafficSource source : group.get().getTrafficSources()) {
        Instance instance = new Instance(agent.getEc2().getInstanceId().get());
        results.add(getLoadBalancer(source.getType()).registerInstance(instance, source.getName(), agent));
      }
      if (results.contains(RegisterInstanceResult.ELB_NO_VPC_FOUND) && configuration.get().isFailWhenNoElbForVpc()) {
        throw new NoMatchingElbForVpcException(String.format("No ELB found for vpc %s", agent.getEc2().getVpcId().or("")));
      }
    }
  }

  public boolean isElbEnabledAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) {
    if (group.isPresent()) {
      if (!group.get().getTrafficSources().isEmpty()) {
        if (agent.getEc2().getInstanceId().isPresent()) {
          return true;
        } else {
          LOG.debug("No instance id for agent {}, can't add to ELB", agent.getAgentId());
        }
      } else {
        LOG.debug("No traffic sources for group {}, not adding agent {} to an ELB", group.get().getName(), agent.getAgentId());
      }
    } else {
      LOG.debug("Group {} not found for agent {}", groupName, agent.getAgentId());
    }
    return false;
  }

  public void syncAll() {
    Collection<BaragonGroup> groups = loadBalancerDatastore.getLoadBalancerGroups();
    classicLoadBalancer.syncAll(groups);
    applicationLoadBalancer.syncAll(groups);
  }

  private ElasticLoadBalancer getLoadBalancer(TrafficSourceType type) {
    switch (type) {
      case ALB_TARGET_GROUP:
        return applicationLoadBalancer;
      case CLASSIC:
      default:
        return classicLoadBalancer;
    }
  }
}

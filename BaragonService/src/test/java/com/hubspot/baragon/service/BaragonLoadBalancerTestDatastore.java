package com.hubspot.baragon.service;

import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import org.apache.curator.framework.CuratorFramework;

public class BaragonLoadBalancerTestDatastore extends BaragonLoadBalancerDatastore {
  private Optional<Set<String>> loadBalancerGroupsOverride = Optional.absent();
  private Optional<Collection<BaragonAgentMetadata>> loadBalancerAgentsOverride = Optional.absent();

  @Inject
  public BaragonLoadBalancerTestDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public void setLoadBalancerGroupsOverride(Optional<Set<String>> loadBalancerGroupsOverride) {
    this.loadBalancerGroupsOverride = loadBalancerGroupsOverride;
  }

  public void setLoadBalancerAgentsOverride(Optional<Collection<BaragonAgentMetadata>> loadBalancerAgentsOverride) {
    this.loadBalancerAgentsOverride = loadBalancerAgentsOverride;
  }

  @Override
  public Set<String> getLoadBalancerGroupNames() {
    if (loadBalancerGroupsOverride.isPresent()) {
      return loadBalancerGroupsOverride.get();
    }

    return super.getLoadBalancerGroupNames();
  }

  @Override
  public Collection<BaragonAgentMetadata> getAgentMetadata(Collection<String> clusterNames) {
    if (loadBalancerAgentsOverride.isPresent()) {
      return loadBalancerAgentsOverride.get();
    } else {
      return super.getAgentMetadata(clusterNames);
    }
  }
}

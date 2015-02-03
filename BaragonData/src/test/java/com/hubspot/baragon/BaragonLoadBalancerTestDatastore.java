package com.hubspot.baragon;

import org.apache.curator.framework.CuratorFramework;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;

public class BaragonLoadBalancerTestDatastore extends BaragonLoadBalancerDatastore {
  private Optional<Set<String>> loadBalancerGroupsOverride = Optional.absent();

  @Inject
  public BaragonLoadBalancerTestDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public void setLoadBalancerGroupsOverride(Optional<Set<String>> loadBalancerGroupsOverride) {
    this.loadBalancerGroupsOverride = loadBalancerGroupsOverride;
  }

  @Override
  public Set<String> getLoadBalancerGroups() {
    if (loadBalancerGroupsOverride.isPresent()) {
      return loadBalancerGroupsOverride.get();
    }

    return super.getLoadBalancerGroups();
  }
}

package com.hubspot.baragon.service.cache;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.google.common.base.Optional;

public class ElbInfo {
  private long descriptionUpdatedAt;
  private Optional<LoadBalancerDescription> loadBalancerDescription = Optional.absent();
  private long statesUpdatedAt = 0;
  private List<InstanceState> instanceStates = new ArrayList<>();

  public ElbInfo(LoadBalancerDescription loadBalancerDescription) {
    this.loadBalancerDescription = Optional.of(loadBalancerDescription);
    this.descriptionUpdatedAt = System.currentTimeMillis();
  }

  public ElbInfo(List<InstanceState> instanceStates) {
    this.instanceStates = instanceStates;
    this.statesUpdatedAt = System.currentTimeMillis();
  }

  public Optional<LoadBalancerDescription> getLoadBalancerDescription() {
    return loadBalancerDescription;
  }

  public void setLoadBalancerDescription(Optional<LoadBalancerDescription> loadBalancerDescription) {
    this.loadBalancerDescription = loadBalancerDescription;
  }

  public void setAZs(List<String> availabilityZones) {
    if (loadBalancerDescription.isPresent()) {
      loadBalancerDescription.get().getAvailabilityZones().clear();
      loadBalancerDescription.get().getAvailabilityZones().addAll(availabilityZones);
    }
    this.descriptionUpdatedAt = System.currentTimeMillis();
  }

  public void setSubnets(List<String> subnets) {
    if (loadBalancerDescription.isPresent()) {
      loadBalancerDescription.get().getSubnets().clear();
      loadBalancerDescription.get().getSubnets().addAll(subnets);
    }
    this.descriptionUpdatedAt = System.currentTimeMillis();
  }

  public void addInstances(List<Instance> instances) {
    if (loadBalancerDescription.isPresent()) {
      loadBalancerDescription.get().getInstances().addAll(instances);
    }
    this.descriptionUpdatedAt = System.currentTimeMillis();
  }

  public void removeInstances(List<Instance> instances) {
    if (loadBalancerDescription.isPresent()) {
      loadBalancerDescription.get().getInstances().removeAll(instances);
    }
  }

  public List<InstanceState> getInstanceStates() {
    return instanceStates;
  }

  public void setInstanceStates(List<InstanceState> instanceStates) {
    this.instanceStates = instanceStates;
    this.statesUpdatedAt = System.currentTimeMillis();
  }

  public long getDescriptionUpdatedAt() {
    return descriptionUpdatedAt;
  }

  public long getStatesUpdatedAt() {
    return statesUpdatedAt;
  }
}

package com.hubspot.baragon.service.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;

@Singleton
public class ElbCache {
  private final AmazonElasticLoadBalancingClient elbClient;
  private final Optional<ElbConfiguration> elbConfiguration;
  private ConcurrentHashMap<String, ElbInfo> elbs = new ConcurrentHashMap<>();
  private AtomicLong allDescriptionsUpdatedAt = new AtomicLong(0);

  @Inject
  public ElbCache(Optional<ElbConfiguration> elbConfiguration,
                  @Named(BaragonServiceModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient) {
    this.elbConfiguration = elbConfiguration;
    this.elbClient = elbClient;
  }

  public Optional<LoadBalancerDescription> getElbInfo(String name) {
    if (elbs.containsKey(name)) {
      if (elbs.get(name).getLoadBalancerDescription().isPresent() && !isCachedValueTooOld(elbs.get(name).getDescriptionUpdatedAt())) {
        return elbs.get(name).getLoadBalancerDescription();
      } else {
        return updateElbInfo(name);
      }
    } else {
      return updateElbInfo(name);
    }
  }

  private synchronized Optional<LoadBalancerDescription> updateElbInfo(String name) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(Collections.singletonList(name));
    DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
    if (!result.getLoadBalancerDescriptions().isEmpty()) {
      LoadBalancerDescription description = result.getLoadBalancerDescriptions().get(0);
      if (elbs.containsKey(name)) {
        elbs.get(name).setLoadBalancerDescription(Optional.of(description));
      } else {
        elbs.put(name, new ElbInfo(description));
      }
      return Optional.of(description);
    } else {
      if (elbs.containsKey(name)) {
        elbs.get(name).setLoadBalancerDescription(Optional.<LoadBalancerDescription>absent());
      }
      return Optional.absent();
    }
  }

  public List<InstanceState> getInstanceStates(String name) {
    if (elbs.containsKey(name)) {
      if (elbs.get(name).getStatesUpdatedAt() < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(elbConfiguration.get().getMaxInstanceStateCacheAgeMinutes())) {
        return elbs.get(name).getInstanceStates();
      } else {
        return updateInstanceStates(name);
      }
    } else {
      return updateInstanceStates(name);
    }
  }

  private synchronized List<InstanceState> updateInstanceStates(String name) {
    DescribeInstanceHealthRequest describeRequest = new DescribeInstanceHealthRequest(name);
    DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(describeRequest);
    if (elbs.containsKey(name)) {
      elbs.get(name).setInstanceStates(result.getInstanceStates());
    } else {
      elbs.put(name, new ElbInfo(result.getInstanceStates()));
    }
    return result.getInstanceStates();
  }

  public List<LoadBalancerDescription> descriptionsForGroup(BaragonGroup group) {
    boolean allUpToDate = true;
    List<LoadBalancerDescription> descriptions = new ArrayList<>();
    List<String> notFound = new ArrayList<>(group.getSources());
    for (Map.Entry<String, ElbInfo> entry : elbs.entrySet()) {
      if (group.getSources().contains(entry.getKey())) {
        if (entry.getValue().getLoadBalancerDescription().isPresent() && !isCachedValueTooOld(entry.getValue().getDescriptionUpdatedAt())) {
          descriptions.add(entry.getValue().getLoadBalancerDescription().get());
          notFound.remove(entry.getKey());
        } else {
          allUpToDate = false;
          break;
        }
      }
    }
    if (!notFound.isEmpty()) {
      allUpToDate = false;
    }
    if (allUpToDate) {
      return descriptions;
    } else {
      return updateDescriptionsForGroup(group.getSources());
    }
  }

  private synchronized List<LoadBalancerDescription> updateDescriptionsForGroup(Set<String> elbNames) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(new ArrayList<>(elbNames));
    List<LoadBalancerDescription> descriptions = elbClient.describeLoadBalancers(request).getLoadBalancerDescriptions();

    for (LoadBalancerDescription description : descriptions) {
      if (elbs.containsKey(description.getLoadBalancerName())) {
        elbs.get(description.getLoadBalancerName()).setLoadBalancerDescription(Optional.of(description));
      } else {
        elbs.put(description.getLoadBalancerName(), new ElbInfo(description));
      }
    }
    return descriptions;
  }

  private boolean isCachedValueTooOld(long time) {
    return time < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(elbConfiguration.get().getMaxDescriptionCacheAgeMinutes());
  }

  public RegisterInstancesWithLoadBalancerResult addToElb(String elbName, Instance instance) throws AmazonClientException {
    RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Collections.singletonList(instance));
    return addToElb(request);
  }

  public synchronized RegisterInstancesWithLoadBalancerResult addToElb(RegisterInstancesWithLoadBalancerRequest request) {
    RegisterInstancesWithLoadBalancerResult result = elbClient.registerInstancesWithLoadBalancer(request);
    elbs.get(request.getLoadBalancerName()).addInstances(request.getInstances());
    return result;
  }

  public DeregisterInstancesFromLoadBalancerResult removeFromElb(String elbName, Instance instance) throws AmazonClientException {
    DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(elbName, Collections.singletonList(instance));
    return removeFromElb(request);
  }

  public synchronized DeregisterInstancesFromLoadBalancerResult removeFromElb(DeregisterInstancesFromLoadBalancerRequest request) {
    DeregisterInstancesFromLoadBalancerResult result = elbClient.deregisterInstancesFromLoadBalancer(request);
    elbs.get(request.getLoadBalancerName()).removeInstances(request.getInstances());
    return result;
  }

  public synchronized void enableAZ(String availabilityZone, LoadBalancerDescription description) {
    List<String> availabilityZones = description.getAvailabilityZones();
    availabilityZones.add(availabilityZone);
    EnableAvailabilityZonesForLoadBalancerRequest request = new EnableAvailabilityZonesForLoadBalancerRequest();
    request.setAvailabilityZones(availabilityZones);
    request.setLoadBalancerName(description.getLoadBalancerName());
    elbClient.enableAvailabilityZonesForLoadBalancer(request);
    elbs.get(description.getLoadBalancerName()).setAZs(request.getAvailabilityZones());
  }

  public synchronized void addSubnet(BaragonAgentMetadata agent, LoadBalancerDescription description) {
    AttachLoadBalancerToSubnetsRequest request = new AttachLoadBalancerToSubnetsRequest();
    request.setLoadBalancerName(description.getLoadBalancerName());
    List<String> subnets = description.getSubnets();
    subnets.add(agent.getEc2().getSubnetId().get());
    request.setSubnets(subnets);
    elbClient.attachLoadBalancerToSubnets(request);
    elbs.get(request.getLoadBalancerName()).setSubnets(request.getSubnets());
  }

  public synchronized List<LoadBalancerDescription> updateAllDescriptions() {
    List<LoadBalancerDescription> descriptions = elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
    List<String> updatedNames = new ArrayList<>();
    for (LoadBalancerDescription description : descriptions) {
      if (elbs.containsKey(description.getLoadBalancerName())) {
        elbs.get(description.getLoadBalancerName()).setLoadBalancerDescription(Optional.of(description));
      } else {
        elbs.put(description.getLoadBalancerName(), new ElbInfo(description));
      }
      updatedNames.add(description.getLoadBalancerName());
    }

    List<String> toRemove = new ArrayList<>();
    for (String elbName : elbs.keySet()) {
      if (!updatedNames.contains(elbName)) {
        toRemove.add(elbName);
      }
    }
    for (String nameToRemove :toRemove) {
      elbs.remove(nameToRemove);
    }
    allDescriptionsUpdatedAt.set(System.currentTimeMillis());
    return descriptions;
  }

  public List<LoadBalancerDescription> getAllDescriptions() {
    if (!isCachedValueTooOld(allDescriptionsUpdatedAt.get())) {
      List<LoadBalancerDescription> allDescriptions = new ArrayList<>();
      for (ElbInfo info : elbs.values()) {
        if (info.getLoadBalancerDescription().isPresent()) {
          allDescriptions.add(info.getLoadBalancerDescription().get());
        }
      }
      return allDescriptions;
    } else {
      return updateAllDescriptions();
    }
  }

  public void clear() {
    elbs.clear();
    allDescriptionsUpdatedAt.set(0);
  }
}

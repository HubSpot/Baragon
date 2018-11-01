package com.hubspot.baragon.service.elb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.RegisterBy;
import com.hubspot.baragon.models.TrafficSource;
import com.hubspot.baragon.models.TrafficSourceState;
import com.hubspot.baragon.models.TrafficSourceType;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;

public class ClassicLoadBalancer extends ElasticLoadBalancer {
  private static final Logger LOG = LoggerFactory.getLogger(ClassicLoadBalancer.class);
  private final AmazonElasticLoadBalancingClient elbClient;

  @Inject
  public ClassicLoadBalancer(Optional<ElbConfiguration> configuration,
                             BaragonExceptionNotifier exceptionNotifier,
                             BaragonLoadBalancerDatastore loadBalancerDatastore,
                             BaragonKnownAgentsDatastore knownAgentsDatastore,
                             @Named(BaragonServiceModule.BARAGON_AWS_ELB_CLIENT_V1) AmazonElasticLoadBalancingClient elbClient) {
    super(configuration, exceptionNotifier, loadBalancerDatastore, knownAgentsDatastore);
    this.elbClient = elbClient;
  }

  public boolean isInstanceHealthy(String instanceId, String elbName) {
    DescribeInstanceHealthRequest describeRequest = new DescribeInstanceHealthRequest(elbName);
    DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(describeRequest);
    boolean instanceIsHealthy = false;
    for (InstanceState instanceState : result.getInstanceStates()) {
      if (instanceState.getState().equals("InService")) {
        if (instanceState.getInstanceId().equals(instanceId)) {
          instanceIsHealthy = true;
        }
      }
    }
    return instanceIsHealthy;
  }

  public AgentCheckInResponse removeInstance(Instance instance, String id, String elbName, String agentId) {
    Optional<LoadBalancerDescription> elb = getElb(elbName);
    if (elb.isPresent()) {
      if (elb.get().getInstances().contains(instance)) {
        DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(elbName, Arrays.asList(instance));
        elbClient.deregisterInstancesFromLoadBalancer(request);
        LOG.info("Deregistered instance {} from ELB {}", request.getInstances(), request.getLoadBalancerName());
      } else {
        LOG.debug("Agent {} already de-registered from ELB {}", agentId, elbName);
      }
    }
    return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
  }

  public AgentCheckInResponse registerInstance(Instance instance, String id, String elbName, BaragonAgentMetadata agent) {
    Optional<String> maybeException = Optional.absent();
    Optional<LoadBalancerDescription> elb = getElb(elbName);
    if (elb.isPresent()) {
      if (isVpcOk(agent, elb.get())) {
        if (!elb.get().getInstances().contains(instance)) {
          checkAZEnabled(agent, elbName, elb.get());
          RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance));
          elbClient.registerInstancesWithLoadBalancer(request);
          LOG.info("Registered instances {} with ELB {}", request.getInstances(), request.getLoadBalancerName());
        } else {
          LOG.debug("Agent {} already registered with ELB {}", agent.getAgentId(), elbName);
        }
      } else {
        maybeException = Optional.of(String.format("No ELB found for vpc %s", agent.getEc2().getVpcId()));
      }
    }
    return new AgentCheckInResponse(TrafficSourceState.DONE, maybeException, 0L);
  }

  public AgentCheckInResponse checkRegisteredInstance(Instance instance, TrafficSource trafficSource, BaragonAgentMetadata agent) {
    return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
  }

  public AgentCheckInResponse checkRemovedInstance(Instance instance, String elbName, String agentId) {
    return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
  }

  public void syncAll(Collection<BaragonGroup> groups) {
    try {
      List<LoadBalancerDescription> elbs = elbClient.describeLoadBalancers().getLoadBalancerDescriptions();

      for (BaragonGroup group : groups) {
        if (!group.getTrafficSources().isEmpty()) {
          List<LoadBalancerDescription> elbsForGroup = getElbsForGroup(elbs, group);
          LOG.debug("Registering new instances for group {}...", group.getName());
          registerNewInstances(elbsForGroup, group);
          if (configuration.get().isDeregisterEnabled()) {
            LOG.debug("Deregistering old instances for group {}...", group.getName());
            deregisterOldInstances(elbsForGroup, group);
          }
          LOG.debug("ELB sync complete for group: {}", group.getName());
        } else {
          LOG.debug("No traffic sources present for group: {}", group.getName());
        }
      }
    } catch (AmazonClientException e) {
      LOG.error("Could not retrieve elb information due to amazon client error %s", e);
      exceptionNotifier.notify(e, ImmutableMap.of("groups", groups == null ? "" : groups.toString()));
    } catch (Exception e) {
      LOG.error("Could not process elb sync due to error {}", e);
      exceptionNotifier.notify(e, ImmutableMap.of("groups", groups == null ? "" : groups.toString()));
    }
  }

  private boolean isVpcOk(BaragonAgentMetadata agent, LoadBalancerDescription elb) {
    if (agent.getEc2().getVpcId().isPresent()) {
      return agent.getEc2().getVpcId().get().equals(elb.getVPCId()) || !configuration.get().isCheckForCorrectVpc();
    } else {
      return !configuration.get().isCheckForCorrectVpc();
    }
  }

  private void checkAZEnabled(BaragonAgentMetadata agent, String elbName, List<LoadBalancerDescription> elbs) {
    for (LoadBalancerDescription elb : elbs) {
      checkAZEnabled(agent, elbName, elb);
    }
  }

  private void checkAZEnabled(BaragonAgentMetadata agent, String elbName,LoadBalancerDescription elb) {
    if (agent.getEc2().getAvailabilityZone().isPresent()) {
      String availabilityZone = agent.getEc2().getAvailabilityZone().get();
      if (elb.getLoadBalancerName().equals(elbName) && !elb.getAvailabilityZones().contains(availabilityZone)) {
        try {
          if (agent.getEc2().getSubnetId().isPresent()) {
            addSubnet(agent, elb);
          } else {
            enableAZ(agent, availabilityZone, elb);
          }
        } catch (AmazonClientException e) {
          LOG.error("Could not enable availability zone {} for elb {} due to error", availabilityZone, elb.getLoadBalancerName(), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", elbName, "subnet", agent.getEc2().getSubnetId().toString(), "availabilityZone", availabilityZone));
        }
      }
    } else {
      LOG.warn("No availability zone specified for agent {}", agent.getAgentId());
    }
  }

  private void addSubnet(BaragonAgentMetadata agent, LoadBalancerDescription elb) {
    LOG.info("Enabling subnet {} in preparation for agent {}", agent.getEc2().getSubnetId().get(), agent.getAgentId());
    AttachLoadBalancerToSubnetsRequest request = new AttachLoadBalancerToSubnetsRequest();
    request.setLoadBalancerName(elb.getLoadBalancerName());
    List<String> subnets = elb.getSubnets();
    subnets.add(agent.getEc2().getSubnetId().get());
    request.setSubnets(subnets);
    elbClient.attachLoadBalancerToSubnets(request);
  }

  private void enableAZ(BaragonAgentMetadata agent, String availabilityZone, LoadBalancerDescription elb) {
    LOG.info("Enabling availability zone {} in preparation for agent {}", availabilityZone, agent.getAgentId());
    List<String> availabilityZones = elb.getAvailabilityZones();
    availabilityZones.add(availabilityZone);
    EnableAvailabilityZonesForLoadBalancerRequest request = new EnableAvailabilityZonesForLoadBalancerRequest();
    request.setAvailabilityZones(availabilityZones);
    request.setLoadBalancerName(elb.getLoadBalancerName());
    elbClient.enableAvailabilityZonesForLoadBalancer(request);
  }

  private List<LoadBalancerDescription> getElbsForGroup(List<LoadBalancerDescription> elbs, BaragonGroup group) {
    List<LoadBalancerDescription> elbsForGroup = new ArrayList<>();
    for (LoadBalancerDescription elb : elbs) {
      List<String> trafficSourceNames = new ArrayList<>();
      for (TrafficSource trafficSource : group.getTrafficSources()) {
        if (trafficSource.getType() == TrafficSourceType.CLASSIC) {
          trafficSourceNames.add(trafficSource.getName());
        }
      }

      if (trafficSourceNames.contains(elb.getLoadBalancerName())) {
        elbsForGroup.add(elb);
      }
    }
    return elbsForGroup;
  }

  private void registerNewInstances(List<LoadBalancerDescription> elbs, BaragonGroup group) {
    Collection<BaragonAgentMetadata> agents = loadBalancerDatastore.getAgentMetadata(group.getName());
    List<RegisterInstancesWithLoadBalancerRequest> requests = registerRequests(group, agents, elbs);
    if (!requests.isEmpty()) {
      for (RegisterInstancesWithLoadBalancerRequest request : requests) {
        try {
          elbClient.registerInstancesWithLoadBalancer(request);
          LOG.info("Registered instances {} with ELB {}", request.getInstances(), request.getLoadBalancerName());
        } catch (AmazonClientException e) {
          LOG.error("Could not register {} with elb {} due to error", request.getInstances(), request.getLoadBalancerName(), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", request.getLoadBalancerName(), "toAdd", request.getInstances().toString()));
        }
      }
    } else {
      LOG.debug("No new instances to register for group {}", group.getName());
    }
  }

  private List<RegisterInstancesWithLoadBalancerRequest> registerRequests(BaragonGroup group, Collection<BaragonAgentMetadata> agents, List<LoadBalancerDescription> elbs) {
    List<RegisterInstancesWithLoadBalancerRequest> requests = new ArrayList<>();
    for (BaragonAgentMetadata agent : agents) {
      try {
        for (TrafficSource source : group.getTrafficSources()) {
          if (source.getType() != TrafficSourceType.CLASSIC) {
            continue;
          }
          if (agent.getEc2().getInstanceId().isPresent()) {
            if (shouldRegister(agent, source.getName(), elbs)) {
              Instance instance = new Instance(agent.getEc2().getInstanceId().get());
              requests.add(new RegisterInstancesWithLoadBalancerRequest(source.getName(), Arrays.asList(instance)));
              checkAZEnabled(agent, source.getName(), elbs);
              LOG.info("Will register {}-{} with ELB {}", agent.getAgentId(), agent.getEc2().getInstanceId().get(), source.getName());
            } else {
              LOG.debug("Agent {} is already registered", agent);
            }
          } else {
            throw new IllegalArgumentException(String.format("Agent Instance Id must be present to register with an ELB (agent: %s)", agent.getAgentId()));
          }
        }
      } catch (Exception e) {
        LOG.error("Could not create request for BaragonAgent {} due to error: {}", agent, e);
      }
    }
    return requests;
  }

  private boolean shouldRegister(BaragonAgentMetadata agent, String elbName, List<LoadBalancerDescription> elbs) {
    Optional<LoadBalancerDescription> matchingElb = Optional.absent();
    for (LoadBalancerDescription elb : elbs) {
      if (elbName.equals(elb.getLoadBalancerName())) {
        matchingElb = Optional.of(elb);
      }
    }
    if (!matchingElb.isPresent()) {
      return false;
    }

    boolean alreadyRegistered = false;
    for (Instance instance : matchingElb.get().getInstances()) {
      if (agent.getEc2().getInstanceId().get().equals(instance.getInstanceId())) {
        alreadyRegistered = true;
      }
    }

    return !alreadyRegistered && (isVpcOk(agent, matchingElb.get()) || !configuration.get().isCheckForCorrectVpc());
  }

  private void deregisterOldInstances(List<LoadBalancerDescription> elbs, BaragonGroup group) {
    Collection<BaragonAgentMetadata> agents = loadBalancerDatastore.getAgentMetadata(group.getName());
    try {
      List<DeregisterInstancesFromLoadBalancerRequest> requests = deregisterRequests(group, agents, elbs);
      for (DeregisterInstancesFromLoadBalancerRequest request : requests) {
        try {
          if (configuration.get().isRemoveLastHealthyEnabled() || !isLastHealthyInstance(request)) {
            elbClient.deregisterInstancesFromLoadBalancer(request);
          } else {
            LOG.info("Will not deregister {} because it is the last healthy instance!", request.getInstances());
          }
          LOG.info("Deregistered instances {} from ELB {}", request.getInstances(), request.getLoadBalancerName());
        } catch (AmazonClientException e) {
          LOG.error("Could not deregister {} from elb {} due to error {}", request.getInstances(), request.getLoadBalancerName(), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", request.getLoadBalancerName(), "toRemove", request.getInstances().toString()));
        }
      }
    } catch (Exception e) {
      LOG.error("Will not try to deregister due to error: {}", e);
    }
  }

  private List<DeregisterInstancesFromLoadBalancerRequest> deregisterRequests(BaragonGroup group, Collection<BaragonAgentMetadata> agents, List<LoadBalancerDescription> elbs) {
    List<String> agentInstanceIds = agentInstanceIds(agents);
    List<DeregisterInstancesFromLoadBalancerRequest> requests = new ArrayList<>();
    for (LoadBalancerDescription elb : elbs) {
      if (group.getTrafficSources().contains(new TrafficSource(elb.getLoadBalancerName(), TrafficSourceType.CLASSIC, RegisterBy.INSTANCE_ID))) {
        for (Instance instance : elb.getInstances()) {
          if (!agentInstanceIds.contains(instance.getInstanceId()) && canDeregisterAgent(group, instance.getInstanceId())) {
            List<Instance> instanceList = new ArrayList<>(1);
            instanceList.add(instance);
            requests.add(new DeregisterInstancesFromLoadBalancerRequest(elb.getLoadBalancerName(), instanceList));
            LOG.info("Will deregister instance {} from ELB {}", instance.getInstanceId(), elb.getLoadBalancerName());
          }
        }
      }
    }
    return requests;
  }

  private boolean isLastHealthyInstance(DeregisterInstancesFromLoadBalancerRequest request) throws AmazonClientException {
    DescribeInstanceHealthRequest describeRequest = new DescribeInstanceHealthRequest(request.getLoadBalancerName());
    DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(describeRequest);
    boolean instanceIsHealthy = false;
    int healthyCount = 0;
    for (InstanceState instanceState : result.getInstanceStates()) {
      if (instanceState.getState().equals("InService")) {
        healthyCount++;
        if (instanceState.getInstanceId().equals(request.getInstances().get(0).getInstanceId())) { //Will only ever be one instance per request
          instanceIsHealthy = true;
        }
      }
    }
    return (instanceIsHealthy && healthyCount == 1);
  }

  private Optional<LoadBalancerDescription> getElb(String elbName) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(Arrays.asList(elbName));
    DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
    if (!result.getLoadBalancerDescriptions().isEmpty()) {
      return Optional.of(result.getLoadBalancerDescriptions().get(0));
    } else {
      return Optional.absent();
    }
  }
}

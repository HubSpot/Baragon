package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.exceptions.NoMatchingElbForVpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ElbManager {
  private static final Logger LOG = LoggerFactory.getLogger(ElbManager.class);

  private final AmazonElasticLoadBalancingClient elbClient;
  private final Optional<ElbConfiguration> configuration;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonExceptionNotifier exceptionNotifier;

  @Inject
  public ElbManager(BaragonLoadBalancerDatastore loadBalancerDatastore,
                    BaragonKnownAgentsDatastore knownAgentsDatastore,
                    Optional<ElbConfiguration> configuration,
                    BaragonExceptionNotifier exceptionNotifier,
                    @Named(BaragonServiceModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient) {
    this.elbClient = elbClient;
    this.configuration = configuration;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.exceptionNotifier = exceptionNotifier;
  }

  public boolean isElbConfigured() {
    return (configuration.isPresent() && configuration.get().isEnabled());
  }

  public boolean isActiveAndHealthy(Optional<BaragonGroup> group, BaragonAgentMetadata agent) {
    for (String elbName : group.get().getSources()) {
      if (isHealthyInstance(agent.getEc2().getInstanceId().get(), elbName)) {
        return true;
      }
    }
    return false;
  }

  public void attemptRemoveAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) throws AmazonClientException {
    if (elbEnabledAgent(agent, group, groupName)) {
      for (String elbName : group.get().getSources()) {
        Instance instance = new Instance(agent.getEc2().getInstanceId().get());
        Optional<LoadBalancerDescription> elb = elbByName(elbName);
        if (elb.isPresent()) {
          if (elb.get().getInstances().contains(instance)) {
            DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(elbName, Arrays.asList(instance));
            elbClient.deregisterInstancesFromLoadBalancer(request);
            LOG.info(String.format("Deregistered instance %s from ELB %s", request.getInstances(), request.getLoadBalancerName()));
          } else {
            LOG.debug(String.format("Agent %s already registered with ELB %s", agent.getAgentId(), elbName));
          }
        } else {

        }
      }
    }
  }

  public void attemptAddAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) throws AmazonClientException, NoMatchingElbForVpcException {
    if (elbEnabledAgent(agent, group, groupName)) {
      boolean matchingElbFound = false;
      boolean matchingElbAndVpcFound = false;
      for (String elbName : group.get().getSources()) {
        Instance instance = new Instance(agent.getEc2().getInstanceId().get());
        Optional<LoadBalancerDescription> elb = elbByName(elbName);
        if (elb.isPresent()) {
          matchingElbFound = true;
          if (isVpcOk(agent, elb.get()) && !elb.get().getInstances().contains(instance)) {
            matchingElbAndVpcFound = true;
            checkAZEnabled(agent, elbName, elb.get());
            RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance));
            elbClient.registerInstancesWithLoadBalancer(request);
            LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
          } else {
            LOG.debug(String.format("Agent %s already registered with ELB %s", agent.getAgentId(), elbName));
          }
        }
      }
      if (matchingElbFound && !matchingElbAndVpcFound && configuration.get().isFailWhenNoElbForVpc()) {
        throw new NoMatchingElbForVpcException(String.format("No ELB found for vpc %s", agent.getEc2().getVpcId().or("")));
      }
    }
  }

  public boolean elbEnabledAgent(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName) {
    if (group.isPresent()) {
      if (!group.get().getSources().isEmpty()) {
        if (agent.getEc2().getInstanceId().isPresent()) {
          return true;
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

  private Optional<LoadBalancerDescription> elbByName(String elbName) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(Arrays.asList(elbName));
    DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
    if (!result.getLoadBalancerDescriptions().isEmpty()) {
      return Optional.of(result.getLoadBalancerDescriptions().get(0));
    } else {
      return Optional.absent();
    }
  }

  private boolean isVpcOk(BaragonAgentMetadata agent, LoadBalancerDescription elb) {
    if (agent.getEc2().getVpcId().isPresent()) {
      return agent.getEc2().getVpcId().get().equals(elb.getVPCId()) || !configuration.get().isCheckForCorrectVpc();
    } else {
      return !configuration.get().isCheckForCorrectVpc();
    }
  }

  public void syncAll() {
    List<LoadBalancerDescription> elbs = elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
    Collection<BaragonGroup> groups = null;
    try {
      groups = loadBalancerDatastore.getLoadBalancerGroups();
      for (BaragonGroup group : groups) {
        if (!group.getSources().isEmpty()) {
          List<LoadBalancerDescription> elbsForGroup = getElbsForGroup(elbs, group);
          LOG.debug(String.format("Registering new instances for group %s...", group.getName()));
          registerNewInstances(elbsForGroup, group);
          if (configuration.get().isDeregisterEnabled()) {
            LOG.debug(String.format("Deregistering old instances for group %s...", group.getName()));
            deregisterOldInstances(elbsForGroup, group);
          }
          LOG.debug(String.format("ELB sync complete for group: %s", group.getName()));
        } else {
          LOG.debug(String.format("No traffic sources present for group: %s", group.getName()));
        }
      }
    } catch (AmazonClientException e) {
      LOG.error(String.format("Could not retrieve elb information due to amazon client error %s", e));
      exceptionNotifier.notify(e, ImmutableMap.of("groups", groups == null ? "" : groups.toString()));
    } catch (Exception e) {
      LOG.error(String.format("Could not process elb sync due to error %s", e));
      exceptionNotifier.notify(e, ImmutableMap.of("groups", groups == null ? "" : groups.toString()));
    }
  }

  private List<LoadBalancerDescription> getElbsForGroup(List<LoadBalancerDescription> elbs, BaragonGroup group) {
    List<LoadBalancerDescription> elbsForGroup = new ArrayList<>();
    for (LoadBalancerDescription elb : elbs) {
      if (group.getSources().contains(elb.getLoadBalancerName())) {
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
          LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
        } catch (AmazonClientException e) {
          LOG.error(String.format("Could not register %s with elb %s due to error", request.getInstances(), request.getLoadBalancerName()), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", request.getLoadBalancerName(), "toAdd", request.getInstances().toString()));
        }
      }
    } else {
      LOG.debug(String.format("No new instances to register for group %s", group.getName()));
    }
  }

  private List<RegisterInstancesWithLoadBalancerRequest> registerRequests(BaragonGroup group, Collection<BaragonAgentMetadata> agents, List<LoadBalancerDescription> elbs) {
    List<RegisterInstancesWithLoadBalancerRequest> requests = new ArrayList<>();
    for (BaragonAgentMetadata agent : agents) {
      try {
        for (String elbName : group.getSources()) {
          if (agent.getEc2().getInstanceId().isPresent()) {
            if (shouldRegister(agent, elbName, elbs)) {
              Instance instance = new Instance(agent.getEc2().getInstanceId().get());
              requests.add(new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance)));
              checkAZEnabled(agent, elbName, elbs);
              LOG.info(String.format("Will register %s-%s with ELB %s", agent.getAgentId(), agent.getEc2().getInstanceId().get(), elbName));
            } else {
              LOG.debug(String.format("Agent %s is already registered", agent));
            }
          } else {
            throw new IllegalArgumentException(String.format("Agent Instance Id must be present to register with an ELB (agent: %s)", agent.getAgentId()));
          }
        }
      } catch (Exception e) {
        LOG.error(String.format("Could not create request for BaragonAgent %s due to error: %s", agent, e));
      }
    }
    return requests;
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
            enabledAZ(agent, availabilityZone, elb);
          }
        } catch (AmazonClientException e) {
          LOG.error("Could not enable availability zone %s for elb %s due to error", availabilityZone, elb.getLoadBalancerName(), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", elbName, "subnet", agent.getEc2().getSubnetId().toString(), "availabilityZone", availabilityZone));
        }
      }
    } else {
      LOG.warn(String.format("No availability zone specified for agent %s", agent.getAgentId()));
    }
  }

  private void addSubnet(BaragonAgentMetadata agent, LoadBalancerDescription elb) {
    LOG.info(String.format("Enabling subnet %s in preparation for agent %s", agent.getEc2().getSubnetId().get(), agent.getAgentId()));
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

    if (!alreadyRegistered) {
      return isVpcOk(agent, matchingElb.get()) || !configuration.get().isCheckForCorrectVpc();
    } else {
      return true;
    }
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
            LOG.info(String.format("Will not deregister %s because it is the last healthy instance!", request.getInstances()));
          }
          LOG.info(String.format("Deregistered instances %s from ELB %s", request.getInstances(), request.getLoadBalancerName()));
        } catch (AmazonClientException e) {
          LOG.error("Could not deregister %s from elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
          exceptionNotifier.notify(e, ImmutableMap.of("elb", request.getLoadBalancerName(), "toRemove", request.getInstances().toString()));
        }
      }
    } catch (Exception e) {
      LOG.error(String.format("Will not try to deregister due to error: %s", e));
    }
  }

  private List<DeregisterInstancesFromLoadBalancerRequest> deregisterRequests(BaragonGroup group, Collection<BaragonAgentMetadata> agents, List<LoadBalancerDescription> elbs) {
    List<String> agentInstances = agentInstanceIds(agents);
    List<DeregisterInstancesFromLoadBalancerRequest> requests = new ArrayList<>();
    for (LoadBalancerDescription elb : elbs) {
      if (group.getSources().contains(elb.getLoadBalancerName())) {
        for (Instance instance : elb.getInstances()) {
          if (!agentInstances.contains(instance.getInstanceId()) && canDeregisterAgent(group, instance)) {
            List<Instance> instanceList = new ArrayList<>(1);
            instanceList.add(instance);
            requests.add(new DeregisterInstancesFromLoadBalancerRequest(elb.getLoadBalancerName(), instanceList));
            LOG.info(String.format("Will deregister instance %s from ELB %s", instance.getInstanceId(), elb.getLoadBalancerName()));
          }
        }
      }
    }
    return requests;
  }

  private boolean canDeregisterAgent(BaragonGroup group, Instance instance) {
    Optional<BaragonKnownAgentMetadata>  agent = knownAgent(group, instance);
    if (!agent.isPresent()) {
      return true;
    } else {
      if (configuration.get().isRemoveKnownAgentEnabled()) {
        Date lastSeen = new Date(agent.get().getLastSeenAt());
        Date threshold = new Date(System.currentTimeMillis() - (configuration.get().getRemoveKnownAgentMinutes() * 60000L));
        return lastSeen.before(threshold);
      } else {
        return false;
      }
    }
  }

  private List<String> agentInstanceIds(Collection<BaragonAgentMetadata> agents) {
    List<String> instanceIds = new ArrayList<>();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getEc2().getInstanceId().isPresent()) {
        instanceIds.add(agent.getEc2().getInstanceId().get());
      } else {
        throw new IllegalArgumentException(String.format("Cannot have an absent Agent Instance Id (agent: %s)", agent.getAgentId()));
      }
    }
    return instanceIds;
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

  private boolean isHealthyInstance(String instanceId, String loadBalancerName) throws AmazonClientException {
    DescribeInstanceHealthRequest describeRequest = new DescribeInstanceHealthRequest(loadBalancerName);
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

  private Optional<BaragonKnownAgentMetadata> knownAgent(BaragonGroup group, Instance instance) {
    Collection<BaragonKnownAgentMetadata> knownAgents = knownAgentsDatastore.getKnownAgentsMetadata(group.getName());
    for (BaragonKnownAgentMetadata agent : knownAgents) {
      if (agent.getEc2().getInstanceId().isPresent() && agent.getEc2().getInstanceId().get().equals(instance.getInstanceId())) {
        return Optional.of(agent);
      }
    }
    return Optional.absent();
  }
}

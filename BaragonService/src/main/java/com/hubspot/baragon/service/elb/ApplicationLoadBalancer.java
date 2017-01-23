package com.hubspot.baragon.service.elb;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.SetSubnetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthStateEnum;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.TrafficSource;
import com.hubspot.baragon.models.TrafficSourceType;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;

/**
 * Handles interactions with the ApplicationLoadBalancer in AWS.
 *
 * For this class I am working under the assumption that the ID's of Targets can
 * be interchanged with the ID's of instances. That is, if you ask this class
 * for the health of instance 12345, then you will actually expect the health
 * of target 12345.
 */
public class ApplicationLoadBalancer extends ElasticLoadBalancer {
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationLoadBalancer.class);
  private final AmazonElasticLoadBalancingClient elbClient;

  @Inject
  public ApplicationLoadBalancer(Optional<ElbConfiguration> configuration,
                                 BaragonExceptionNotifier exceptionNotifier,
                                 BaragonLoadBalancerDatastore loadBalancerDatastore,
                                 BaragonKnownAgentsDatastore knownAgentsDatastore,
                                 @Named(BaragonServiceModule.BARAGON_AWS_ELB_CLIENT_V2) AmazonElasticLoadBalancingClient elbClient) {
    super(configuration, exceptionNotifier, loadBalancerDatastore, knownAgentsDatastore);
    this.elbClient = elbClient;
  }

  @Override
  public boolean isInstanceHealthy(String instanceId, String elbName) {
    DescribeTargetHealthRequest healthRequest = new DescribeTargetHealthRequest()
        .withTargets(new TargetDescription().withId(instanceId));
    DescribeTargetHealthResult result = elbClient.describeTargetHealth(healthRequest);

    for (TargetHealthDescription health: result.getTargetHealthDescriptions()) {
      if (health.getTargetHealth().getState().equals(TargetHealthStateEnum.Healthy.toString())
          && health.getTarget().getId().equals(instanceId)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void removeInstance(Instance instance, String elbName, String agentId) {
    /* TODO: Need to map v1 Instance -> v2 Target */
  }

  @Override
  public RegisterInstanceResult registerInstance(Instance instance, String elbName, BaragonAgentMetadata agent) {
    /* TODO: Need to map v1 Instance -> v2 Target */
    return RegisterInstanceResult.ELB_AND_VPC_FOUND;
  }

  @Override
  public void syncAll(Collection<BaragonGroup> baragonGroups) {
    Collection<LoadBalancer> allLoadBalancers = getAllLoadBalancers(baragonGroups);
    for (BaragonGroup baragonGroup : baragonGroups) {
      if (baragonGroup.getSources().isEmpty()) {
        LOG.debug("No traffic sources present for group {}", baragonGroup.getName());
      }
      else {
        try {
          Collection<LoadBalancer> elbsForBaragonGroup = getLoadBalancersByBaragonGroup(allLoadBalancers, baragonGroup);
          Collection<BaragonAgentMetadata> baragonAgents = getAgentsByBaragonGroup(baragonGroup);


          LOG.debug("Looking for TargetGroup for baragon group {}", baragonGroup.getName());
          Optional<TargetGroup> maybeTargetGroup = guaranteeTargetGroupFor(baragonGroup);
          if (maybeTargetGroup.isPresent()) {
            TargetGroup targetGroup = maybeTargetGroup.get();
            Collection<TargetDescription> targets = targetsInTargetGroup(targetGroup);

            LOG.debug("Registering new instances for group {}", baragonGroup.getName());
            guaranteeRegistered(targetGroup, targets, baragonAgents, elbsForBaragonGroup);

            if (configuration.isPresent() && configuration.get().isDeregisterEnabled()) {
              LOG.debug("De-registering old instances for group {}", baragonGroup.getName());
              deregisterRemovableTargets(baragonGroup, targetGroup, baragonAgents, targets);
            }
          } else {
            LOG.debug("No TargetGroup for Baragon Group {}", baragonGroup);
          }

          LOG.debug("ELB sync complete for group {}", baragonGroup);
        } catch (AmazonClientException acexn) {
          LOG.error("Could not retrieve elb information due to ELB client error {}", acexn);
          exceptionNotifier.notify(acexn, ImmutableMap.of("baragonGroup", baragonGroup.toString()));
        } catch (Exception exn) {
          LOG.error("Could not process ELB sync due to error {}", exn);
          exceptionNotifier.notify(exn, ImmutableMap.of("groups", baragonGroup.toString()));
        }
      }
    }
  }

  private Collection<LoadBalancer> getAllLoadBalancers(Collection<BaragonGroup> baragonGroups) {
    Set<String> trafficSources = new HashSet<>();
    for (BaragonGroup baragonGroup : baragonGroups) {
      for (TrafficSource trafficSource : baragonGroup.getSources()) {
        if (trafficSource.getType() == TrafficSourceType.APPLICATION) {
          trafficSources.add(trafficSource.getName());
        }
      }
    }

    Collection<LoadBalancer> loadBalancers = new HashSet<>();
    DescribeLoadBalancersRequest loadBalancersRequest = new DescribeLoadBalancersRequest()
        .withNames(trafficSources);
    DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(loadBalancersRequest);
    String nextPage = result.getNextMarker();
    loadBalancers.addAll(result.getLoadBalancers());

    while (!Strings.isNullOrEmpty(nextPage)) {
      loadBalancersRequest = new DescribeLoadBalancersRequest()
          .withMarker(nextPage);
      result = elbClient.describeLoadBalancers(loadBalancersRequest);
      nextPage = result.getNextMarker();
      loadBalancers.addAll(result.getLoadBalancers());
    }

    return loadBalancers;
  }

  private Collection<LoadBalancer> getLoadBalancersByBaragonGroup(Collection<LoadBalancer> allLoadBalancers, BaragonGroup baragonGroup) {
    Set<TrafficSource> trafficSources = baragonGroup.getSources();
    Set<String> trafficSourceNames = new HashSet<>();
    Collection<LoadBalancer> loadBalancersForGroup = new HashSet<>();

    for (TrafficSource trafficSource : trafficSources) {
      trafficSourceNames.add(trafficSource.getName());
    }

    for (LoadBalancer loadBalancer : allLoadBalancers) {
      if (trafficSourceNames.contains(loadBalancer.getLoadBalancerName())) {
        loadBalancersForGroup.add(loadBalancer);
      }
    }

    return loadBalancersForGroup;
  }

  /**
   * Ensure that the given baragon agent is attached to the given target group. When this function
   * completes, the baragon agent will be attached to the load balancer, whether or not it originally
   * was.
   *
   * @param baragonAgents BaragonAgent to register with given load balancer
   * @param loadBalancers Load balancer to register with
   */
  private void guaranteeRegistered(TargetGroup targetGroup,
                                   Collection<TargetDescription> targets,
                                   Collection<BaragonAgentMetadata> baragonAgents,
                                   Collection<LoadBalancer> loadBalancers) {
    /*
    - Check that load balancers, baragon agents, target groups are on same VPC
    - Check that load balancers, targets are on same subnet (== AZ)
    - Check that all baragon agents are associated with a target on target group
    - Check that load balancers has listeners, rules to make talk to target group
     */

    if (configuration.isPresent() && configuration.get().isCheckForCorrectVpc()) {
      guaranteeSameVPC(targetGroup, baragonAgents, loadBalancers);
    }

    guaranteeAzEnabled(baragonAgents, loadBalancers);
    guaranteeHasAllTargets(targetGroup, targets, baragonAgents);
    //guaranteeListenersPresent(targetGroup, loadBalancers);
  }

  /**
   * De-register any targets representing agents that are not known to the BaragonService,
   * or which otherwise need to be removed.
   *
   * @param targetGroup TargetGroup to check for old agents
   * @param agents Known agents, to be used as a reference sheet
   */
  private void deregisterRemovableTargets(BaragonGroup baragonGroup, TargetGroup targetGroup,
                                          Collection<BaragonAgentMetadata> agents,
                                          Collection<TargetDescription> targets) {
    Collection<TargetDescription> removableTargets = listRemovableTargets(baragonGroup, targets, agents);

    for (TargetDescription removableTarget : removableTargets) {
      try {
        if (isLastHealthyInstance(removableTarget, targetGroup)
            && configuration.isPresent()
            && !configuration.get().isRemoveLastHealthyEnabled()) {
          LOG.info("Will not de-register target {} because it is last healthy instance in {}", removableTarget, targetGroup);
        } else {
          elbClient.deregisterTargets(new DeregisterTargetsRequest()
              .withTargetGroupArn(targetGroup.getTargetGroupArn())
              .withTargets(removableTarget));
          LOG.info("De-registered target {} from target group {}", removableTarget, targetGroup);
        }
      } catch (AmazonClientException acexn) {
        LOG.error("Could not de-register target {} from target group {} due to error",
            removableTarget, targetGroup, acexn);
        exceptionNotifier.notify(acexn, ImmutableMap
            .of("targetGroup", targetGroup.getTargetGroupName()));
      }
    }
  }

  /**
   * When this method completes, the target group, the agents, and the loadBalancers are
   * all on the same VPC.
   * The target group, each of the agents, and each of the load balancers should think
   * that they are on the same VPC, otherwise they won't be able to talk to each other.
   *
   *
   * @param targetGroup Group - and consequently all targets - to check
   * @param agents Agents to check
   * @param loadBalancers Load balances to check
   */
  private void guaranteeSameVPC(TargetGroup targetGroup,
                                Collection<BaragonAgentMetadata> agents,
                                Collection<LoadBalancer> loadBalancers) {
    String vpcId = targetGroup.getVpcId();

    for (BaragonAgentMetadata agent : agents) {
      if (agent.getEc2().getVpcId().isPresent()) {
        if (! agent.getEc2().getVpcId().get().equals(vpcId)) {
          LOG.error("Agent {} not on same VPC as its target group {}", agent, targetGroup);
          throw new IllegalStateException(String.format("Agent %s not on same VPC as its target group %s", agent, targetGroup));
        }
      } else {
        LOG.error("Agent {} not assigned to a VPC", agent);
        throw new IllegalStateException(String.format("Agent %s not assigned to a VPC", agent));
      }
    }

    for (LoadBalancer loadBalancer : loadBalancers) {
      if (!vpcId.equals(loadBalancer.getVpcId())) {
        LOG.error("Load balancer {} on different VPC from target group {}", loadBalancer, targetGroup);
        throw new IllegalStateException(String.format("Load balancer %s on different VPC from target group %s", loadBalancer, targetGroup));
      }
    }
  }

  private void guaranteeAzEnabled(Collection<BaragonAgentMetadata> agents,
                                  Collection<LoadBalancer> loadBalancers) {
    for (LoadBalancer loadBalancer : loadBalancers) {
      Collection<String> azNames = new HashSet<>();
      for (AvailabilityZone availabilityZone : loadBalancer.getAvailabilityZones()) {
        azNames.add(availabilityZone.getZoneName());
      }

      for (BaragonAgentMetadata agent : agents) {
        if (agent.getEc2().getAvailabilityZone().isPresent()
            && ! azNames.contains(agent.getEc2().getAvailabilityZone().get())) {
          guaranteeHasAllSubnets(
              agent.getEc2().getSubnetId().asSet(),
              loadBalancer);
        }
      }
    }
  }

  /**
   *
   * @param target Target to check
   * @param targetGroup Group to check in
   * @return if the given target is the last healthy target in the given target group
   */
  private boolean isLastHealthyInstance(TargetDescription target, TargetGroup targetGroup) {
    DescribeTargetHealthRequest targetHealthRequest = new DescribeTargetHealthRequest()
        .withTargetGroupArn(targetGroup.getTargetGroupArn());
    List<TargetHealthDescription> targetHealthDescriptions = elbClient
        .describeTargetHealth(targetHealthRequest)
        .getTargetHealthDescriptions();

    boolean instanceIsHealthy = false;
    int healthyCount = 0;

    for (TargetHealthDescription targetHealthDescription : targetHealthDescriptions) {
      if (targetHealthDescription.getTargetHealth().getState()
          .equals(TargetHealthStateEnum.Healthy.toString())) {
        healthyCount += 1;
        if (targetHealthDescription.getTarget().equals(target)) {
          instanceIsHealthy = true;
        }
      }
    }

    return instanceIsHealthy && healthyCount == 1;
  }

  private void guaranteeHasAllSubnets(Collection<String> subnetIds, LoadBalancer loadBalancer) {
    Collection<String> subnetsOnLoadBalancer = getSubnetsFromLoadBalancer(loadBalancer);
    Set<String> subnetsToAdd = new HashSet<>();
    for (String subnetId : subnetIds) {
      if (! subnetsOnLoadBalancer.contains(subnetId)) {
        subnetsToAdd.add(subnetId);
      }
    }

    subnetsToAdd = Sets.union(new HashSet<>(subnetsOnLoadBalancer), subnetsToAdd);

    try {
      SetSubnetsRequest subnetsRequest = new SetSubnetsRequest()
          .withLoadBalancerArn(loadBalancer.getLoadBalancerArn())
          .withSubnets(subnetsToAdd);
      elbClient.setSubnets(subnetsRequest);
    } catch (AmazonClientException acexn) {
      LOG.error("Could not attach subnets {} to load balancer {} due to error",
          subnetsToAdd, loadBalancer.getLoadBalancerName(), acexn);
      exceptionNotifier.notify(acexn, ImmutableMap.of(
          "elb", loadBalancer.getLoadBalancerName(),
          "subnets", subnetsToAdd.toString()));
    }
  }

  /**
   * After this method completes, every agent in baragonAgents should be associated with
   * a target in the given target group.
   *
   * @param targetGroup group to register in
   * @param baragonAgents agents to be registered
   */
  private void guaranteeHasAllTargets(TargetGroup targetGroup,
                                      Collection<TargetDescription> targets,
                                      Collection<BaragonAgentMetadata> baragonAgents) {
    Collection<TargetDescription> targetDescriptions = new HashSet<>();
    for (BaragonAgentMetadata agent : baragonAgents) {
      try {
        if (agent.getEc2().getInstanceId().isPresent()) {
          if (agentShouldRegisterInTargetGroup(agent.getEc2().getInstanceId().get(), targetGroup, targets)) {
            String instanceId = agent.getEc2().getInstanceId().get();
            targetDescriptions.add(new TargetDescription().withId(instanceId));
            LOG.info("Will register agent {} to target in group {}", agent, targetGroup);
          } else {
            LOG.debug("Agent {} is already registered", agent);
          }
        } else {
          throw new IllegalArgumentException(
              String.format("Agent instance ID must be present to register with an ELB (Agent %s)",
                  agent.toString()));
        }
      } catch (Exception exn) {
        LOG.error("Could not create request to register agent {} due to error", agent, exn);
        exceptionNotifier.notify(exn, ImmutableMap.of("agent", agent.toString()));
      }
    }

    if (targetDescriptions.isEmpty()) {
      LOG.debug("No new instances to register with target group");
    } else {
      try {
        RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest()
            .withTargetGroupArn(targetGroup.getTargetGroupArn())
            .withTargets(targetDescriptions);
        elbClient.registerTargets(registerTargetsRequest);
        LOG.info("Registered targets {} onto target group {}", targetDescriptions, targetGroup);
      } catch (AmazonClientException acexn) {
        LOG.error("Failed to register targets {} onto target group {}", targetDescriptions, targetGroup);
        exceptionNotifier.notify(acexn, ImmutableMap.of(
            "targets", targetDescriptions.toString(),
            "targetGroup", targetGroup.toString()));
      }
    }
  }

  private boolean agentShouldRegisterInTargetGroup(String baragonAgentInstanceId, TargetGroup targetGroup, Collection<TargetDescription> targets) {
    boolean shouldRegister = true;
    for (TargetDescription target : targets) {
      if (target.getId().equals(baragonAgentInstanceId)) {
        shouldRegister = false;
      }
    }
    return shouldRegister;
  }

  private Collection<String> getSubnetsFromLoadBalancer(LoadBalancer loadBalancer) {
    List<AvailabilityZone> availabilityZones = loadBalancer.getAvailabilityZones();
    Set<String> subnetIds = new HashSet<>();

    for (AvailabilityZone availabilityZone : availabilityZones) {
      subnetIds.add(availabilityZone.getSubnetId());
    }

    return subnetIds;
  }

  private Optional<TargetGroup> guaranteeTargetGroupFor(BaragonGroup baragonGroup) {
    DescribeTargetGroupsRequest targetGroupsRequest = new DescribeTargetGroupsRequest()
        .withNames(baragonGroup.getName());
    List<TargetGroup> targetGroups = elbClient.describeTargetGroups(targetGroupsRequest)
        .getTargetGroups();
    if (targetGroups.isEmpty()) {
      LOG.info("No target group set up for BaragonGroup {}. Skipping.", baragonGroup);
      return Optional.absent();
    } else {
      return Optional.of(targetGroups.get(0));
    }
  }

  private Collection<TargetDescription> targetsInTargetGroup(TargetGroup targetGroup) {
    DescribeTargetHealthRequest targetHealthRequest = new DescribeTargetHealthRequest()
        .withTargetGroupArn(targetGroup.getTargetGroupArn());
    List<TargetHealthDescription> targetGroupsResult = elbClient
        .describeTargetHealth(targetHealthRequest)
        .getTargetHealthDescriptions();

    Collection<TargetDescription> targetDescriptions = new HashSet<>();
    for (TargetHealthDescription targetHealthDescription : targetGroupsResult) {
      targetDescriptions.add(targetHealthDescription.getTarget());
    }

    return targetDescriptions;
  }

  private Collection<TargetDescription> listRemovableTargets(BaragonGroup baragonGroup,
                                                             Collection<TargetDescription> targetsOnGroup,
                                                             Collection<BaragonAgentMetadata> agentsInBaragonGroup) {
    Collection<String> agentIds = instanceIds(agentsInBaragonGroup);

    Collection<TargetDescription> removableTargets = new HashSet<>();
    for (TargetDescription targetDescription : targetsOnGroup) {
      if (! agentIds.contains(targetDescription.getId()) && canDeregisterAgent(baragonGroup, targetDescription.getId())) {
        LOG.info("Will attempt to deregister target {}", targetDescription.getId());
        removableTargets.add(targetDescription);
      }
    }

    return removableTargets;
  }

  private Collection<String> instanceIds(Collection<BaragonAgentMetadata> agents) {
    Collection<String> instanceIds = new HashSet<>();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getEc2().getInstanceId().isPresent()) {
        instanceIds.add(agent.getEc2().getInstanceId().get());
      }
    }
    return instanceIds;
  }

  private Collection<BaragonAgentMetadata> getAgentsByBaragonGroup(BaragonGroup baragonGroup) {
    return loadBalancerDatastore.getAgentMetadata(baragonGroup.getName());
  }
}

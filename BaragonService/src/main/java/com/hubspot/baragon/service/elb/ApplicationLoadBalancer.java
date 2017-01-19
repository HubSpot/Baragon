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
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.SetSubnetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthStateEnum;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
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
    /* In particular, this needs the name of the TargetGroup (targetGroupARN) to work */
    TargetDescription targetDescription = new TargetDescription()
        .withId(instance.getInstanceId());
    DeregisterTargetsRequest deregisterTargetsRequest = new DeregisterTargetsRequest()
        .withTargetGroupArn("" /* Required */)
        .withTargets(targetDescription /* Required */);
    elbClient.deregisterTargets(deregisterTargetsRequest);
    LOG.info("De-registered target {} from target group {}", targetDescription, null);
  }

  @Override
  public RegisterInstanceResult registerInstance(Instance instance, String elbName, BaragonAgentMetadata agent) {
    /* TODO: Need to map v1 Instance -> v2 Target */
    /* In particular, this needs the name of the TargetGroup (targetGroupARN) to work */
    TargetDescription targetDescription = new TargetDescription()
        .withId(instance.getInstanceId() /* Required */);
    RegisterTargetsRequest registerTargetsRequest = new RegisterTargetsRequest()
        .withTargetGroupArn("" /* Required */)
        .withTargets(targetDescription);
    elbClient.registerTargets(registerTargetsRequest);

    /* TODO: Need to patch up the Listeners / rules / VPC / Subnet / ... */
    return RegisterInstanceResult.ELB_AND_VPC_FOUND;
  }

  @Override
  public void syncAll(Collection<BaragonGroup> baragonGroups) {
    try {
      for (BaragonGroup baragonGroup : baragonGroups) {
        if (baragonGroup.getSources().isEmpty()) {
          LOG.debug("No traffic sources present for group {}", baragonGroup.getName());
        } else {
          Collection<LoadBalancer> elbsForBaragonGroup = getLoadBalancersByBaragonGroup(baragonGroup);
          Collection<BaragonAgentMetadata> baragonAgents = getAgentsByBaragonGroup(baragonGroup);

          LOG.debug("Registering TargetGroup for baragon group {}", baragonGroup.getName());
          TargetGroup targetGroup = guaranteeTargetGroupFor(baragonGroup);


          LOG.debug("Registering new instances for group {}", baragonGroup.getName());
          guaranteeRegistered(targetGroup, baragonAgents, elbsForBaragonGroup);

          if (configuration.isPresent() && configuration.get().isDeregisterEnabled()) {
            LOG.debug("De-registering old instances for group {}", baragonGroup.getName());
            deregisterIfOld(targetGroup, baragonAgents);
          }

          LOG.debug("ELB sync complete for group {}", baragonGroup);
        }
      }
    } catch (AmazonClientException acexn) {
      LOG.error("Could not retrieve elb information due to ELB client error {}", acexn);
      exceptionNotifier.notify(acexn, ImmutableMap.of("groups", baragonGroups == null ? "" : baragonGroups.toString()));
    } catch (Exception exn) {
      LOG.error("Could not process ELB sync due to error {}", exn);
      exceptionNotifier.notify(exn, ImmutableMap.of("groups", baragonGroups == null ? "" : baragonGroups.toString()));
    }
  }

  private Collection<LoadBalancer> getLoadBalancersByBaragonGroup(BaragonGroup baragonGroup) {
    Set<TrafficSource> trafficSources = baragonGroup.getSources();
    Set<String> trafficSourceNames = new HashSet<>();
    for (TrafficSource trafficSource : trafficSources) {
      if (trafficSource.getType() == TrafficSourceType.APPLICATION) {
        trafficSourceNames.add(trafficSource.getName());
      }
    }

    DescribeLoadBalancersRequest loadBalancersRequest = new DescribeLoadBalancersRequest()
        .withNames(trafficSourceNames);
    return elbClient.describeLoadBalancers(loadBalancersRequest)
        .getLoadBalancers();
  }

  /**
   * Ensure that the given baragon agent is attached to the given load balancer. When this function
   * completes, the baragon agent will be attached to the load balancer, whether or not it originally
   * was.
   *
   * @param baragonAgents BaragonAgent to register with given load balancer
   * @param loadBalancers Load balancer to register with
   */
  private void guaranteeRegistered(TargetGroup targetGroup,
                                   Collection<BaragonAgentMetadata> baragonAgents,
                                   Collection<LoadBalancer> loadBalancers) {
    /* TODO */
    guaranteeSameVPC(targetGroup, baragonAgents, loadBalancers);

    /*
    - Check that load balancers, baragon agents, target groups are on same VPC
    - Check that load balancers, baragon agents, target groups are on same subnet
    - Check that all baragon agents are associated with a target on target group
    - Check that load balancers has listeners, rules to make talk to target group

     */

    // if subnet not attached, then attach
    Collection<String> subnetIds = getSubnetIdsFrom(baragonAgents);
    guaranteeHasAllSubnets(subnetIds, loadBalancers);

    guaranteeHasAllTargets(baragonAgents, loadBalancers);

    // if az not active, activate
    // Add missing ec2 instances
    // Attach ec2 instances to group
    /* TODO */
  }

  /**
   * De-register any targets representing agents that are not known to the BaragonService,
   * or which otherwise need to be removed.
   *
   * @param targetGroup TargetGroup to check for old agents
   * @param agents Known agents, to be used as a reference sheet
   */
  private void deregisterIfOld(TargetGroup targetGroup, Collection<BaragonAgentMetadata> agents) {
    Collection<TargetDescription> targets = targetsInTargetGroup(targetGroup);
    Collection<TargetDescription> removeableTargets = listOldTargets(targets, agents);

    for (TargetDescription removeableTarget : removeableTargets) {
      try {
        if (isLastHealthyInstance(removeableTarget, targetGroup)
            && configuration.isPresent()
            && !configuration.get().isRemoveLastHealthyEnabled()) {
          LOG.info("Will not de-register target {} because it is last healthy instance in {}", removeableTarget, targetGroup);
        } else {
          elbClient.deregisterTargets(new DeregisterTargetsRequest()
              .withTargetGroupArn(targetGroup.getTargetGroupArn())
              .withTargets(removeableTarget));
          LOG.info("De-registered target {} from target group {}", removeableTarget, targetGroup);
        }
      } catch (AmazonClientException acexn) {
        LOG.error("Could not de-register target {} from target group {} due to error",
            removeableTarget, targetGroup, acexn);
        exceptionNotifier.notify(acexn, ImmutableMap
            .of("targetGroup", targetGroup.getTargetGroupName()));
      }
    }
  }

  private void guaranteeSameVPC(TargetGroup targetGroup,
                                Collection<BaragonAgentMetadata> agents,
                                Collection<LoadBalancer> loadBalancers) {
    String vpcId = targetGroup.getVpcId();

    for (BaragonAgentMetadata agent : agents) {
      /* TODO */
      agent.getEc2().getVpcId().isPresent();
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

  private Collection<String> getSubnetIdsFrom(Collection<BaragonAgentMetadata> baragonAgents) {
    Set<String> subnetIds = new HashSet<>();
    for (BaragonAgentMetadata baragonAgent : baragonAgents) {
      if (baragonAgent.getEc2().getSubnetId().isPresent()) {
        subnetIds.add(baragonAgent.getEc2().getSubnetId().get());
      }
    }

    return subnetIds;
  }

  private void guaranteeHasAllSubnets(Collection<String> subnetIds, Collection<LoadBalancer> loadBalancers) {
    for (LoadBalancer loadBalancer : loadBalancers) {
      guaranteeHasAllSubnets(subnetIds, loadBalancer);
    }
  }

  private void guaranteeHasAllSubnets(Collection<String> subnetIds, LoadBalancer loadBalancer) {
    Collection<String> subnetsOnLoadBalancer = getSubnetsFromLoadBalancer(loadBalancer);
    Collection<String> subnetsToAdd = new HashSet<>();
    for (String subnetId : subnetIds) {
      if (! subnetsOnLoadBalancer.contains(subnetId)) {
        subnetsToAdd.add(subnetId);
      }
    }

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

  private void guaranteeHasAllTargets(Collection<BaragonAgentMetadata> baragonAgents,
                              Collection<LoadBalancer> loadBalancers) {
    /* TODO */
  }

  private void guaranteeHasAllTargets(Collection<BaragonAgentMetadata> baragonAgents, LoadBalancer loadBalancer) {
    CreateListenerRequest createListenerRequest = new CreateListenerRequest()
        .withLoadBalancerArn(loadBalancer.getLoadBalancerArn());
    CreateRuleRequest createRuleRequest = new CreateRuleRequest();
    CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest()
        .withName("" /* */)
        .withVpcId(loadBalancer.getVpcId());
  }

  private Collection<String> getSubnetsFromLoadBalancer(LoadBalancer loadBalancer) {
    List<AvailabilityZone> availabilityZones = loadBalancer.getAvailabilityZones();
    Set<String> subnetIds = new HashSet<>();

    for (AvailabilityZone availabilityZone : availabilityZones) {
      subnetIds.add(availabilityZone.getSubnetId());
    }

    return subnetIds;
  }

  private TargetGroup guaranteeTargetGroupFor(BaragonGroup baragonGroup) {
    DescribeTargetGroupsRequest targetGroupsRequest = new DescribeTargetGroupsRequest()
        .withNames(baragonGroup.getName());
    List<TargetGroup> targetGroups = elbClient.describeTargetGroups(targetGroupsRequest)
        .getTargetGroups();
    if (targetGroups.isEmpty()) {
      LOG.info("Creating AWS TargetGroup for BaragonGroup {}", baragonGroup);
      CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest()
        /* TODO: Other fields, probably from config file */
          .withName(baragonGroup.getName() /* REQUIRED */)
          .withVpcId("" /* REQUIRED */)
          .withProtocol(ProtocolEnum.HTTPS /* REQUIRED */)
          .withPort(80 /* REQUIRED */);
      return elbClient.createTargetGroup(createTargetGroupRequest)
          .getTargetGroups()
          .get(0);
    } else {
      return targetGroups.get(0);
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

  private Collection<TargetDescription> listOldTargets(Collection<TargetDescription> targetsOnGroup,
                                                 Collection<BaragonAgentMetadata> knownAgents) {
    Collection<String> knownAgentIds = agentIds(knownAgents);

    Collection<TargetDescription> removableTargets = new HashSet<>();
    for (TargetDescription targetDescription : targetsOnGroup) {
      /* TODO: is this reasonable? */
      if (! knownAgentIds.contains(targetDescription.getId()) /* && canDeregisterAgent(...) */) {
        LOG.info("Will attempt to deregister target {}", targetDescription.getId());
        removableTargets.add(targetDescription);
      }
    }

    return removableTargets;
  }

  private Collection<String> agentIds(Collection<BaragonAgentMetadata> agents) {
    Collection<String> agentIds = new HashSet<>();
    for (BaragonAgentMetadata agent : agents) {
      agentIds.add(agent.getAgentId());
    }
    return agentIds;
  }

  private Collection<BaragonAgentMetadata> getAgentsByBaragonGroup(BaragonGroup baragonGroup) {
    return loadBalancerDatastore.getAgentMetadata(baragonGroup.getName());
  }

  private boolean checkVpcOk(BaragonAgentMetadata agent, LoadBalancer loadBalancer) {
    if (agent.getEc2().getVpcId().isPresent()) {
      String vpcId = agent.getEc2().getVpcId().get();
      return vpcId.equals(loadBalancer.getVpcId())
          || !(configuration.isPresent()
               && configuration.get().isCheckForCorrectVpc());
    } else {
      return !(configuration.isPresent()
               && configuration.get().isCheckForCorrectVpc());
    }
  }
}

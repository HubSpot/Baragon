package com.hubspot.baragon.service.elb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
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
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;

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

  public boolean isInstanceHealthy(String instanceId, String name) {
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

  public Optional<LoadBalancer> getElb(String elbName) {
    DescribeLoadBalancersRequest loadBalancersRequest = new DescribeLoadBalancersRequest()
        .withNames(elbName);
    DescribeLoadBalancersResult loadBalancersResult = elbClient.describeLoadBalancers(loadBalancersRequest);

    if (loadBalancersResult.getLoadBalancers().isEmpty()) {
      return Optional.absent();
    } else {
      return Optional.of(loadBalancersResult.getLoadBalancers().get(0));
    }
  }

  public void removeInstance(Instance instance, String elbName, String agentId) {

  }

  public RegisterInstanceResult registerInstance(Instance instance, String elbName, BaragonAgentMetadata agent) {
    return RegisterInstanceResult.ELB_AND_VPC_FOUND;
  }

  public void syncAll(Collection<BaragonGroup> baragonGroups) {
    try {

      for (BaragonGroup baragonGroup : baragonGroups) {
        if (baragonGroup.getSources().isEmpty()) {
          LOG.debug("No traffic sources present for group {}", baragonGroup.getName());
        } else {
          Collection<?> elbsForGroup;
          LOG.debug("Registering new instances for group {}", baragonGroup.getName());
          // create TargetGroup if missing
          // if subnet not attached, then attach
          // if az not active, activate
          // Add missing ec2 instances
          // Attach ec2 instances to group
          // deregister old instances
        }
      }

      throw new AmazonClientException("TEMP");
    } catch (AmazonClientException acexn) {
      LOG.error("Could not retrieve elb information due to ELB client error {}", acexn);
      exceptionNotifier.notify(acexn, ImmutableMap.of("groups", baragonGroups == null ? "" : baragonGroups.toString()));
    } catch (Exception exn) {
      LOG.error("Could not process ELB sync due to error {}", exn);
      exceptionNotifier.notify(exn, ImmutableMap.of("groups", baragonGroups == null ? "" : baragonGroups.toString()));
    }
  }

  private Collection<LoadBalancer> getLoadBalancers() {
    return Collections.emptyList();
  }

  private Collection<LoadBalancer> filterLoadBalancersByBaragonGroup(BaragonGroup baragonGroup, Collection<LoadBalancer> allLoadBalancers) {
    List<LoadBalancer> elbsForGroup = new ArrayList<>();
    for (LoadBalancer loadBalancer : allLoadBalancers) {
      if (baragonGroup.getSources().contains(
          TrafficSource.fromString(loadBalancer.getLoadBalancerName()))) {
        elbsForGroup.add(loadBalancer);
      }
    }

    return elbsForGroup;
  }


  private void createTargetGroupFor(BaragonGroup baragonGroup) {
    LOG.info("Creating AWS TargetGroup for BaragonGroup {}", baragonGroup);
    CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest()
        /* TODO: Other fields, probably from config file */
        .withName(baragonGroup.getName());
    elbClient.createTargetGroup(createTargetGroupRequest);
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

  private void addSubnet(BaragonAgentMetadata agent, LoadBalancer loadBalancer) {
    /* TODO */
  }

  private void checkAvailabilityZoneEnabled(BaragonAgentMetadata agent, String elbName, LoadBalancer loadBalancer) {
    if (agent.getEc2().getAvailabilityZone().isPresent()) {
      String availibilityZone = agent.getEc2().getAvailabilityZone().get();

      if (loadBalancer.getLoadBalancerName().equals(elbName)
          && ! loadBalancer.getAvailabilityZones().contains(new AvailabilityZone().withZoneName(availibilityZone))) {
        try {
          if (agent.getEc2().getSubnetId().isPresent()) {
            /* TODO: Attach load balancer to subnet */
          } else {
            /* TODO: Add availability zone to load balancer */
          }
        } catch (AmazonClientException acexn) {
          LOG.error("Could not enable availability zone {} for LoadBalancer {} due to error", availibilityZone, loadBalancer, acexn);
          exceptionNotifier.notify(acexn, ImmutableMap.of(
                  "elb", elbName,
                  "subnet", agent.getAgentId()));
        }
      }

    } else {
      LOG.warn("No availability zone specified for agent {}", agent.getAgentId());
    }
  }
}

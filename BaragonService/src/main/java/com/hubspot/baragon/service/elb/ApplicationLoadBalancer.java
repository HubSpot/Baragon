package com.hubspot.baragon.service.elb;

import java.util.Collection;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;

public class ApplicationLoadBalancer extends ElasticLoadBalancer {
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
    return true;
  }

  public Optional<LoadBalancer> getElb(String elbName) {
    return Optional.absent();
  }

  public void removeInstance(Instance instance, String elbName, String agentId) {

  }

  public RegisterInstanceResult registerInstance(Instance instance, String elbName, BaragonAgentMetadata agent) {
    return RegisterInstanceResult.ELB_AND_VPC_FOUND;
  }

  public void syncAll(Collection<BaragonGroup> groups) {

  }
}

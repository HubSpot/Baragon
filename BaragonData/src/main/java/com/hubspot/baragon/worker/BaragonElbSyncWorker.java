package com.hubspot.baragon.worker;

import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;

public class BaragonElbSyncWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonElbSyncWorker.class);

  private final AmazonElasticLoadBalancingClient elbClient;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public BaragonElbSyncWorker(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              @Named(BaragonDataModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient) {
    this.elbClient = elbClient;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @Override
  public void run() {
    LOG.info("Starting ELB sync");
    Collection<LoadBalancerDescription> elbs = elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
    Collection<BaragonAgentMetadata> agents = loadBalancerDatastore.getAgentMetadata(loadBalancerDatastore.getLoadBalancerGroups());
    LOG.info("Registering new instances...");
    registerNewInstances(elbs, agents);
    LOG.info("Deregistering old instances...");
    deregisterOldInstances(elbs, agents);
  }

  private void registerNewInstances(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    for (RegisterInstancesWithLoadBalancerRequest request : registerRequests(elbs, agents)) {
      elbClient.registerInstancesWithLoadBalancer(request);
      LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
    }
  }

  private Collection<RegisterInstancesWithLoadBalancerRequest> registerRequests(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    LOG.info(elbs.toString());
    LOG.info(agents.toString());
    Collection<RegisterInstancesWithLoadBalancerRequest> requests = Collections.emptyList();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getElbConfiguration().isPresent()) {
        for (String elbName : agent.getElbConfiguration().get().getElbNames()) {
          if (!isRegistered(agent.getElbConfiguration().get().getInstanceId(), elbName, elbs)) {
            Instance instance = new Instance(agent.getElbConfiguration().get().getInstanceId());
            requests.add(new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance)));
            LOG.info(String.format("Will register %s-%s with ELB %s", agent.getAgentId(), agent.getElbConfiguration().get().getInstanceId(), elbName));
          }
        }
      }
    }
    return requests;
  }

  private boolean isRegistered(String instanceId, String elbName, Collection<LoadBalancerDescription> elbs) {
    for (LoadBalancerDescription elb : elbs) {
      for (Instance instance : elb.getInstances()) {
        if (instanceId.equals(instance.getInstanceId()) && elbName.equals(elb.getLoadBalancerName())) {
          return true;
        }
      }
    }
    return false;
  }

  private void deregisterOldInstances(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    for (DeregisterInstancesFromLoadBalancerRequest request : deregisterRequests(elbs, agents)) {
      elbClient.deregisterInstancesFromLoadBalancer(request);
      LOG.info(String.format("Deregistered instances %s from ELB %s", request.getInstances(), request.getLoadBalancerName()));
    }
  }

  private Collection<DeregisterInstancesFromLoadBalancerRequest> deregisterRequests(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    List<String> agentInstances = agentInstanceIds(agents);
    Collection<DeregisterInstancesFromLoadBalancerRequest> requests = Collections.emptyList();
    for (LoadBalancerDescription elb : elbs) {
      for (Instance instance : elb.getInstances()) {
        if (!agentInstances.contains(instance.getInstanceId())) {
          requests.add(new DeregisterInstancesFromLoadBalancerRequest(elb.getLoadBalancerName(),Arrays.asList(instance)));
          LOG.info(String.format("Will deregister instance %s from ELB %s", instance.getInstanceId(), elb.getLoadBalancerName()));
        }
      }
    }
    return requests;
  }

  private List<String> agentInstanceIds(Collection<BaragonAgentMetadata> agents) {
    List<String> instanceIds = Collections.emptyList();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getElbConfiguration().isPresent()) {
        instanceIds.add(agent.getElbConfiguration().get().getInstanceId());
      }
    }
    return instanceIds;
  }
}

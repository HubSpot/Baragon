package com.hubspot.baragon.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.ElbConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;

import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;

public class BaragonElbSyncWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonElbSyncWorker.class);

  private final AmazonElasticLoadBalancingClient elbClient;
  private final ElbConfiguration configuration;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonElbSyncWorker(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              ElbConfiguration configuration,
                              @Named(BaragonDataModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient,
                              @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.elbClient = elbClient;
    this.configuration = configuration;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.workerLastStartAt = workerLastStartAt;
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());

    Collection<BaragonAgentMetadata> agents;
    Collection<LoadBalancerDescription> elbs;
    try {
      LOG.info("Starting ELB sync");
      agents = loadBalancerDatastore.getAgentMetadata(loadBalancerDatastore.getLoadBalancerGroups());
      elbs = elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
    } catch (AmazonClientException e) {
      LOG.info(String.format("Could not retrieve elb information due to error %s", e));
      return;
    }
    LOG.info("Registering new instances...");
    registerNewInstances(elbs, agents);
    LOG.info("Deregistering old instances...");
    deregisterOldInstances(elbs, agents);
  }

  private void registerNewInstances(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    for (RegisterInstancesWithLoadBalancerRequest request : registerRequests(elbs, agents)) {
      try {
        elbClient.registerInstancesWithLoadBalancer(request);
        LOG.info(String.format("Registered instances %s with ELB %s", request.getInstances(), request.getLoadBalancerName()));
      } catch (AmazonClientException e) {
        LOG.info("Could not register %s with elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
      }
    }
  }

  private Collection<RegisterInstancesWithLoadBalancerRequest> registerRequests(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
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
      try {
        if (configuration.canRemoveLastHealthy() || !isLastHealthyInstance(request)) {
          elbClient.deregisterInstancesFromLoadBalancer(request);
        } else {
          LOG.info(String.format("Will not deregister %s because it is the last health instance!", request.getInstances()));
        }
        LOG.info(String.format("Deregistered instances %s from ELB %s", request.getInstances(), request.getLoadBalancerName()));
      } catch (AmazonClientException e) {
        LOG.info("Could not deregister %s from elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
      }
    }
  }

  private Collection<DeregisterInstancesFromLoadBalancerRequest> deregisterRequests(Collection<LoadBalancerDescription> elbs, Collection<BaragonAgentMetadata> agents) {
    List<String> agentInstances = agentInstanceIds(agents);
    Set<String> elbNames = agentElbNames(agents);
    Collection<DeregisterInstancesFromLoadBalancerRequest> requests = Collections.emptyList();
    for (LoadBalancerDescription elb : elbs) {
      for (Instance instance : elb.getInstances()) {
        if (elbNames.contains(elb.getLoadBalancerName()) && !agentInstances.contains(instance.getInstanceId())) {
          List<Instance> instanceList = new ArrayList<>(1);
          instanceList.add(instance);
          requests.add(new DeregisterInstancesFromLoadBalancerRequest(elb.getLoadBalancerName(),instanceList));
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

  private Set<String> agentElbNames(Collection<BaragonAgentMetadata> agents) {
    Set<String> names = Collections.emptySet();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getElbConfiguration().isPresent()) {
        names.addAll(agent.getElbConfiguration().get().getElbNames());
      }
    }
    return names;
  }

  private boolean isLastHealthyInstance(DeregisterInstancesFromLoadBalancerRequest request) {
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
}

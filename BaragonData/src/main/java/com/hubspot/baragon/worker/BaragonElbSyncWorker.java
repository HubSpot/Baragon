package com.hubspot.baragon.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.google.common.base.Optional;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.ElbConfiguration;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;

import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;

public class BaragonElbSyncWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonElbSyncWorker.class);

  private final AmazonElasticLoadBalancingClient elbClient;
  private final Optional<ElbConfiguration> configuration;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonElbSyncWorker(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              BaragonKnownAgentsDatastore knownAgentsDatastore,
                              Optional<ElbConfiguration> configuration,
                              @Named(BaragonDataModule.BARAGON_AWS_ELB_CLIENT) AmazonElasticLoadBalancingClient elbClient,
                              @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.elbClient = elbClient;
    this.configuration = configuration;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.workerLastStartAt = workerLastStartAt;
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());

    List<LoadBalancerDescription> elbs;
    Collection<BaragonGroup> groups;
    try {
      groups = loadBalancerDatastore.getLoadBalancerGroups();
      for (BaragonGroup group : groups) {
        if (!group.getSources().isEmpty()) {
          elbs = elbsForGroup(group);
          LOG.debug(String.format("Registering new instances for group %s...", group.getName()));
          registerNewInstances(elbs, group);
          if (configuration.get().isDeregisterEnabled()) {
            LOG.debug(String.format("Deregistering old instances for group %s...", group.getName()));
            deregisterOldInstances(elbs, group);
          }
          LOG.debug(String.format("ELB sync complete for group: %s", group.getName()));
        } else {
          LOG.debug(String.format("No traffic sources present for group: %s", group.getName()));
        }
      }
    } catch (AmazonClientException e) {
      LOG.error(String.format("Could not retrieve elb information due to amazon client error %s", e));
    } catch (Exception e) {
      LOG.error(String.format("Could not process elb sync due to error %s", e));
    }
    LOG.info("Finished ELB Sync");
  }

  private List<LoadBalancerDescription> elbsForGroup(BaragonGroup group) {
    DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(new ArrayList<>(group.getSources()));
    return elbClient.describeLoadBalancers(request).getLoadBalancerDescriptions();
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
          LOG.error("Could not register %s with elb %s due to error %s", request.getInstances(), request.getLoadBalancerName(), e);
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
          if (agent.getInstanceId().isPresent()) {
            if (!isRegistered(agent.getInstanceId().get(), elbName, elbs)) {
              Instance instance = new Instance(agent.getInstanceId().get());
              requests.add(new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(instance)));
              LOG.info(String.format("Will register %s-%s with ELB %s", agent.getAgentId(), agent.getInstanceId().get(), elbName));
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

  private boolean isRegistered(String instanceId, String elbName, List<LoadBalancerDescription> elbs) {
    for (LoadBalancerDescription elb : elbs) {
      for (Instance instance : elb.getInstances()) {
        if (instanceId.equals(instance.getInstanceId()) && elbName.equals(elb.getLoadBalancerName())) {
          return true;
        }
      }
    }
    return false;
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
      if (agent.getInstanceId().isPresent()) {
        instanceIds.add(agent.getInstanceId().get());
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

  private Optional<BaragonKnownAgentMetadata> knownAgent(BaragonGroup group, Instance instance) {
    Collection<BaragonKnownAgentMetadata> knownAgents = knownAgentsDatastore.getKnownAgentsMetadata(group.getName());
    for (BaragonKnownAgentMetadata agent : knownAgents) {
      if (agent.getInstanceId().isPresent() && agent.getInstanceId().get().equals(instance.getInstanceId())) {
        return Optional.of(agent);
      }
    }
    return Optional.absent();
  }
}

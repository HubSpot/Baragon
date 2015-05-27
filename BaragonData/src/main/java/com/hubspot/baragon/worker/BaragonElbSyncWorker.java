package com.hubspot.baragon.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.google.common.base.Optional;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.ElbConfiguration;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.managers.ElbManager;
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

  private final ElbManager elbManager;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonElbSyncWorker(ElbManager elbManager,
                              @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.elbManager = elbManager;
    this.workerLastStartAt = workerLastStartAt;
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());
    elbManager.syncAll();
    LOG.info("Finished ELB Sync");
  }


}

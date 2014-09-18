package com.hubspot.baragon.agent.managed;

import io.dropwizard.lifecycle.Managed;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.utils.JavaUtils;

public class BootstrapManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapManaged.class);
  
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  
  @Inject
  public BootstrapManaged(BaragonStateDatastore stateDatastore,
                          LoadBalancerConfiguration loadBalancerConfiguration,
                          FilesystemConfigHelper configHelper,
                          @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
  }

  private void applyCurrentConfigs() {
    LOG.info("Loading current state of the world from zookeeper...");

    final Stopwatch stopwatch = new Stopwatch().start();
    final long now = System.currentTimeMillis();

    final Collection<String> services = stateDatastore.getServices();
    LOG.info("Going to apply {} services...", services.size());

    for (BaragonServiceState serviceState : stateDatastore.getGlobalState()) {
      if (serviceState.getService().getLoadBalancerGroups() == null || !serviceState.getService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
        continue;
      }

      LOG.info("    Applying {}: [{}]", serviceState.getService(), JavaUtils.COMMA_JOINER.join(serviceState.getUpstreams()));

      try {
        configHelper.apply(new ServiceContext(serviceState.getService(), serviceState.getUpstreams(), now, true), false);
      } catch (Exception e) {
        LOG.error(String.format("Caught exception while applying %s", serviceState.getService().getServiceId()), e);
      }
    }

    LOG.info("Applied {} services in {}ms", services.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  @Override
  public void start() throws Exception {
    LOG.info("Applying current configs...");
    applyCurrentConfigs();

    LOG.info("Starting leader latch...");
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
  }
}

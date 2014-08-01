package com.hubspot.baragon.agent.managed;

import io.dropwizard.lifecycle.Managed;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;

public class BootstrapManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(BootstrapManaged.class);
  
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
    final long now = System.currentTimeMillis();
    LOG.info("Loading current state of the world from zookeeper...");

    final Stopwatch stopwatch = new Stopwatch();

    stopwatch.start();
    final Collection<String> services = stateDatastore.getServices();

    for (String serviceId : services) {
      final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);
      if (!maybeServiceInfo.isPresent()) {
        continue;  // doubt this will ever happen
      }

      final BaragonService service = maybeServiceInfo.get();
      final Map<String, UpstreamInfo> upstreamsMap = stateDatastore.getUpstreamsMap(serviceId);

      if (service.getLoadBalancerGroups() == null || !service.getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
        LOG.info(String.format("   Skipping %s, not applicable to this LB cluster", serviceId));
        continue;
      }

      LOG.info(String.format("    Applying %s: [%s]", serviceId, Joiner.on(", ").join(upstreamsMap.keySet())));

      try {
        configHelper.apply(new ServiceContext(service, upstreamsMap.values(), now, true), false);
      } catch (Exception e) {
        LOG.error(String.format("Caught exception while applying %s", serviceId), e);
      }
    }

    LOG.info(String.format("Applied %d services in %sms", services.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
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

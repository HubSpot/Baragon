package com.hubspot.baragon.agent.managed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.models.BaragonServiceState;
import io.dropwizard.lifecycle.Managed;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BootstrapManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapManaged.class);

  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonAgentMetadata baragonAgentMetadata;

  @Inject
  public BootstrapManaged(BaragonStateDatastore stateDatastore,
                          BaragonKnownAgentsDatastore knownAgentsDatastore,
                          LoadBalancerConfiguration loadBalancerConfiguration,
                          FilesystemConfigHelper configHelper,
                          @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch,
                          BaragonAgentMetadata baragonAgentMetadata) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.baragonAgentMetadata = baragonAgentMetadata;
  }

  private void applyCurrentConfigs() {
    LOG.info("Loading current state of the world from zookeeper...");

    final Stopwatch stopwatch = Stopwatch.createStarted();
    final long now = System.currentTimeMillis();

    final Collection<String> services = stateDatastore.getServices();
    ExecutorService executorService = Executors.newFixedThreadPool(services.size());
    List<Callable<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>>> todo = new ArrayList<>(services.size());

    for (BaragonServiceState serviceState : stateDatastore.getGlobalState()) {
      if (!(serviceState.getService().getLoadBalancerGroups() == null) && serviceState.getService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
        todo.add(new BootstrapFileChecker(configHelper, serviceState, now));
      }
    }

    LOG.info("Going to apply {} services...", todo.size());

    try {
      List<Future<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>>> applied = executorService.invokeAll(todo);
      for (Future<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>> serviceFuture : applied) {
        Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>> maybeToApply = serviceFuture.get();
        if (maybeToApply.isPresent()) {
          try {
            configHelper.bootstrapApply(maybeToApply.get().getKey(), maybeToApply.get().getValue());
          } catch (Exception e) {
            LOG.error(String.format("Caught exception while applying %s during bootstrap", maybeToApply.get().getKey().getService().getServiceId()), e);
          }
        }
      }
      configHelper.checkAndReload();
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while applying and parsing configs"), e);
    }

    LOG.info("Applied {} services in {}ms", todo.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  @Override
  public void start() throws Exception {
    LOG.info("Applying current configs...");
    applyCurrentConfigs();

    LOG.info("Starting leader latch...");
    leaderLatch.start();

    LOG.info("Adding to known-agents...");
    knownAgentsDatastore.addKnownAgent(loadBalancerConfiguration.getName(), BaragonKnownAgentMetadata.fromAgentMetadata(baragonAgentMetadata, System.currentTimeMillis()));
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
  }
}

package com.hubspot.baragon.agent.managed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.workers.AgentHeartbeatWorker;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.models.BaragonServiceState;
import com.google.common.base.Optional;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import io.dropwizard.lifecycle.Managed;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BootstrapManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapManaged.class);

  private final BaragonAgentConfiguration configuration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final LeaderLatch leaderLatch;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonAgentMetadata baragonAgentMetadata;
  private final ScheduledExecutorService executorService;
  private final AgentHeartbeatWorker agentHeartbeatWorker;
  private final BaragonWorkerDatastore workerDatastore;
  private final HttpClient httpClient;

  private static final String SERVICE_CHECKIN_URL_FORMAT = "%s/checkin/%s/%s";

  private ScheduledFuture<?> requestWorkerFuture = null;

  @Inject
  public BootstrapManaged(BaragonStateDatastore stateDatastore,
                          BaragonKnownAgentsDatastore knownAgentsDatastore,
                          BaragonLoadBalancerDatastore loadBalancerDatastore,
                          BaragonWorkerDatastore workerDatastore,
                          BaragonAgentConfiguration configuration,
                          FilesystemConfigHelper configHelper,
                          AgentHeartbeatWorker agentHeartbeatWorker,
                          BaragonAgentMetadata baragonAgentMetadata,
                          @Named(BaragonAgentServiceModule.AGENT_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                          @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch,
                          @Named(BaragonDataModule.BARAGON_AGENT_HTTP_CLIENT) HttpClient httpClient) {
    this.configuration = configuration;
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.baragonAgentMetadata = baragonAgentMetadata;
    this.executorService = executorService;
    this.agentHeartbeatWorker = agentHeartbeatWorker;
    this.workerDatastore = workerDatastore;
    this.httpClient = httpClient;
  }

  private void applyCurrentConfigs() {
    LOG.info("Loading current state of the world from zookeeper...");

    final Stopwatch stopwatch = Stopwatch.createStarted();
    final long now = System.currentTimeMillis();

    final Collection<String> services = stateDatastore.getServices();
    if (services.size() > 0) {
      ExecutorService executorService = Executors.newFixedThreadPool(services.size());
      List<Callable<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>>> todo = new ArrayList<>(services.size());

      for (BaragonServiceState serviceState : stateDatastore.getGlobalState()) {
        if (!(serviceState.getService().getLoadBalancerGroups() == null) && serviceState.getService().getLoadBalancerGroups().contains(configuration.getLoadBalancerConfiguration().getName())) {
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
    } else {
      LOG.info("No services were found to apply");
    }
  }

  @Override
  public void start() throws Exception {
    LOG.info("Applying current configs...");
    applyCurrentConfigs();

    LOG.info("Starting leader latch...");
    leaderLatch.start();

    LOG.info("Notifying BaragonService...");
    notifyService("startup");

    LOG.info("Updating BaragonGroup information...");
    loadBalancerDatastore.updateGroupInfo(configuration.getLoadBalancerConfiguration().getName(), configuration.getLoadBalancerConfiguration().getDomain());

    LOG.info("Adding to known-agents...");
    knownAgentsDatastore.addKnownAgent(configuration.getLoadBalancerConfiguration().getName(), BaragonKnownAgentMetadata.fromAgentMetadata(baragonAgentMetadata, System.currentTimeMillis()));

    LOG.info("Starting agent heartbeat...");
    requestWorkerFuture = executorService.scheduleAtFixedRate(agentHeartbeatWorker, 0, configuration.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
    notifyService("shutdown");
  }

  private void notifyService(String action) {
    Collection<String> baseUris = workerDatastore.getBaseUris();
    if (!baseUris.isEmpty()) {
      HttpRequest request = HttpRequest.newBuilder()
        .setUrl(String.format(SERVICE_CHECKIN_URL_FORMAT, baseUris.iterator().next(), baragonAgentMetadata.getAgentId(), action))
        .setMethod(HttpRequest.Method.POST)
        .setBody(baragonAgentMetadata)
        .build();
      try {
        HttpResponse response = httpClient.execute(request);
        LOG.info(String.format("Got %s response from BaragonService", response.getStatusCode()));
      } catch (Exception e) {
        LOG.error(String.format("Could not notify service of %s", action), e);
      }
    }
  }
}

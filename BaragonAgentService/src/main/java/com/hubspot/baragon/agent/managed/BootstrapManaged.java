package com.hubspot.baragon.agent.managed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.healthcheck.ConfigChecker;
import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.agent.listeners.ResyncListener;
import com.hubspot.baragon.agent.workers.AgentHeartbeatWorker;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.exceptions.AgentStartupException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpResponse;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapManaged.class);

  private final BaragonAgentConfiguration configuration;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final LeaderLatch leaderLatch;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonAgentMetadata baragonAgentMetadata;
  private final ScheduledExecutorService executorService;
  private final AgentHeartbeatWorker agentHeartbeatWorker;
  private final LifecycleHelper lifecycleHelper;
  private final ConfigChecker configChecker;

  private ScheduledFuture<?> requestWorkerFuture = null;
  private ScheduledFuture<?> configCheckerFuture = null;

  @Inject
  public BootstrapManaged(BaragonKnownAgentsDatastore knownAgentsDatastore,
                          BaragonLoadBalancerDatastore loadBalancerDatastore,
                          BaragonAgentConfiguration configuration,
                          AgentHeartbeatWorker agentHeartbeatWorker,
                          BaragonAgentMetadata baragonAgentMetadata,
                          LifecycleHelper lifecycleHelper,
                          ConfigChecker configChecker,
                          @Named(BaragonAgentServiceModule.AGENT_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                          @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch) {
    this.configuration = configuration;
    this.leaderLatch = leaderLatch;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.baragonAgentMetadata = baragonAgentMetadata;
    this.executorService = executorService;
    this.agentHeartbeatWorker = agentHeartbeatWorker;
    this.lifecycleHelper = lifecycleHelper;
    this.configChecker = configChecker;
  }



  @Override
  public void start() throws Exception {
    LOG.info("Applying current configs...");
    lifecycleHelper.applyCurrentConfigs();

    LOG.info("Starting leader latch...");
    leaderLatch.start();

    if (configuration.isRegisterOnStartup()) {
      LOG.info("Notifying BaragonService...");
      lifecycleHelper.notifyService("startup");
    }

    LOG.info("Updating BaragonGroup information...");
    loadBalancerDatastore.updateGroupInfo(configuration.getLoadBalancerConfiguration().getName(), configuration.getLoadBalancerConfiguration().getDomain());

    LOG.info("Adding to known-agents...");
    knownAgentsDatastore.addKnownAgent(configuration.getLoadBalancerConfiguration().getName(), BaragonKnownAgentMetadata.fromAgentMetadata(baragonAgentMetadata, System.currentTimeMillis()));

    LOG.info("Starting agent heartbeat...");
    requestWorkerFuture = executorService.scheduleAtFixedRate(agentHeartbeatWorker, 0, configuration.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);

    LOG.info("Starting config checker");
    configCheckerFuture = executorService.scheduleAtFixedRate(configChecker, 0, configuration.getConfigCheckIntervalSecs(), TimeUnit.SECONDS);

    lifecycleHelper.writeStateFileIfConfigured();
  }

  @Override
  public void stop() throws Exception {
    lifecycleHelper.shutdown();
  }
}

package com.hubspot.baragon.agent.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

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
import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.ReapplyFailedException;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResyncListener implements ConnectionStateListener {
  private static final Logger LOG = LoggerFactory.getLogger(ResyncListener.class);

  private final BaragonAgentConfiguration configuration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final Lock agentLock;
  private final long agentLockTimeoutMs;

  @Inject
  public ResyncListener(BaragonStateDatastore stateDatastore,
                        FilesystemConfigHelper configHelper,
                        BaragonAgentConfiguration configuration,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.stateDatastore = stateDatastore;
    this.configHelper = configHelper;
    this.configuration = configuration;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (newState.equals(ConnectionState.RECONNECTED)) {
      reapplyConfigsWithRetry();
    }
  }

  private void reapplyConfigsWithRetry() {
    Callable<Void> callable = new Callable<Void>() {
      public Void call() throws Exception {
        if (agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
          applyCurrentConfigs();
          return null;
        } else {
          throw new ReapplyFailedException("Failed to acquire lock to reapply most current configs");
        }
      }
    };

    Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
      .retryIfException()
      .withStopStrategy(StopStrategies.stopAfterAttempt(configuration.getMaxReapplyConfigAttempts()))
      .withWaitStrategy(WaitStrategies.exponentialWait(1, TimeUnit.SECONDS))
      .build();

    try {
      retryer.call(callable);
    } catch (Exception e) {
      // Shut down and exit, we are not in sync
    }
  }

  public void applyCurrentConfigs() {
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
        if (configuration.isExitOnStartupError()) {
          Throwables.propagate(e);
        }
      }

      LOG.info("Applied {} services in {}ms", todo.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    } else {
      LOG.info("No services were found to apply");
    }
  }
}
package com.hubspot.baragon.agent.listeners;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import ch.qos.logback.classic.LoggerContext;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.ServerProvider;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managed.LifecycleHelper;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.exceptions.ReapplyFailedException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.eclipse.jetty.server.Server;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResyncListener implements ConnectionStateListener {
  private static final Logger LOG = LoggerFactory.getLogger(ResyncListener.class);

  private final BaragonAgentConfiguration configuration;

  private final LifecycleHelper lifecycleHelper;
  private final ServerProvider serverProvider;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final Lock agentLock;
  private final long agentLockTimeoutMs;
  private final AtomicReference<String> mostRecentRequestId;

  @Inject
  public ResyncListener(LifecycleHelper lifecycleHelper,
                        BaragonAgentConfiguration configuration,
                        ServerProvider serverProvider,
                        BaragonLoadBalancerDatastore loadBalancerDatastore,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId) {
    this.lifecycleHelper = lifecycleHelper;
    this.configuration = configuration;
    this.serverProvider = serverProvider;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
    this.mostRecentRequestId = mostRecentRequestId;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (newState.equals(ConnectionState.RECONNECTED)) {
      LOG.info("Reconnected to zookeeper, checking if configs are still in sync");
      Optional<String> maybeLastReuqestForGroup = loadBalancerDatastore.getLastRequestForGroup(configuration.getLoadBalancerConfiguration().getName());
      if (!maybeLastReuqestForGroup.isPresent() || maybeLastReuqestForGroup.get().equals(mostRecentRequestId.get())) {
        reapplyConfigsWithRetry();
      }
    }
  }

  private void reapplyConfigsWithRetry() {
    Callable<Void> callable = new Callable<Void>() {
      public Void call() throws Exception {
        if (agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
          lifecycleHelper.applyCurrentConfigs();
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
      abort(e);
    }
  }

  private void abort(Exception exception) {
    LOG.error("Caught exception while trying to resync, aborting", exception);
    flushLogs();
    Optional<Server> server = serverProvider.get();
    if (server.isPresent()) {
      try {
        server.get().stop();
        lifecycleHelper.shutdown();
      } catch (Exception e) {
        LOG.warn("While aborting server", e);
      } finally {
        System.exit(1);
      }
    } else {
      LOG.warn("Baragon Agent abort called before server has fully initialized!");
      System.exit(1);
    }
  }

  private void flushLogs() {
    final long millisToWait = 100;

    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }

    try {
      Thread.sleep(millisToWait);
    } catch (Exception e) {
      LOG.info("While sleeping for log flush", e);
    }
  }
}
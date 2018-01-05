package com.hubspot.baragon.agent.listeners;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managed.LifecycleHelper;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.exceptions.LockTimeoutException;
import com.hubspot.baragon.models.BaragonAgentState;

public class ResyncListener implements ConnectionStateListener {
  private static final Logger LOG = LoggerFactory.getLogger(ResyncListener.class);

  private final BaragonAgentConfiguration configuration;

  private final LifecycleHelper lifecycleHelper;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final ReentrantLock agentLock;
  private final long agentLockTimeoutMs;
  private final AtomicReference<String> mostRecentRequestId;
  private final AtomicReference<BaragonAgentState> agentState;

  @Inject
  public ResyncListener(LifecycleHelper lifecycleHelper,
                        BaragonAgentConfiguration configuration,
                        BaragonLoadBalancerDatastore loadBalancerDatastore,
                        AtomicReference<BaragonAgentState> agentState,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId) {
    this.lifecycleHelper = lifecycleHelper;
    this.configuration = configuration;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.agentState = agentState;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
    this.mostRecentRequestId = mostRecentRequestId;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    switch (newState) {
      case RECONNECTED:
        LOG.info("Reconnected to zookeeper, checking if configs are still in sync");
        Optional<String> maybeLastRequestForGroup = loadBalancerDatastore.getLastRequestForGroup(configuration.getLoadBalancerConfiguration().getName());
        if (!maybeLastRequestForGroup.isPresent() || !maybeLastRequestForGroup.get().equals(mostRecentRequestId.get())) {
          agentState.set(BaragonAgentState.BOOTSTRAPING);
          reapplyConfigsWithRetry();
        }
        agentState.set(BaragonAgentState.ACCEPTING);
        break;
      case SUSPENDED:
      case LOST:
        agentState.set(BaragonAgentState.DISCONNECTED);
        break;
      case CONNECTED:
        agentState.set(BaragonAgentState.ACCEPTING);
        break;
      default:
        break;
    }
  }

  private void reapplyConfigsWithRetry() {
    Callable<Void> callable = new Callable<Void>() {
      public Void call() throws Exception {
        if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
          LOG.warn("Failed to acquire lock for config reapply");
          throw new LockTimeoutException(String.format("Failed to acquire lock to reapply most current configs in %s ms", agentLockTimeoutMs), agentLock);
        }
        try {
          lifecycleHelper.applyCurrentConfigs();
          return null;
        } finally {
          agentLock.unlock();
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
    } catch (RetryException re) {
      LOG.error("Exception applying current configs", re.getLastFailedAttempt().getExceptionCause());
      lifecycleHelper.abort("Caught exception while trying to resync, aborting", re);
    } catch (ExecutionException ee) {
      LOG.error("Exception applying current configs", ee);
      lifecycleHelper.abort("Caught exception while trying to resync, aborting", ee);
    }
  }
}

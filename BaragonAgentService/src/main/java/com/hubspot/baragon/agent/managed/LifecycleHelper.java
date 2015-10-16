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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.classic.LoggerContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.hubspot.baragon.agent.ServerProvider;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.exceptions.AgentStartupException;
import com.hubspot.baragon.exceptions.LockTimeoutException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonAgentState;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.eclipse.jetty.server.Server;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleHelper {
  private static final Logger LOG = LoggerFactory.getLogger(LifecycleHelper.class);

  private static final String SERVICE_CHECKIN_URL_FORMAT = "%s/checkin/%s/%s";
  private static final String GLOBAL_STATE_FORMAT = "%s/state";

  private final BaragonAuthDatastore authDatastore;
  private final BaragonWorkerDatastore workerDatastore;
  private final BaragonAgentConfiguration configuration;
  private final BaragonAgentMetadata baragonAgentMetadata;
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final ServerProvider serverProvider;
  private final AtomicReference<BaragonAgentState> agentState;
  private final HttpClient httpClient;
  private final ScheduledExecutorService executorService;
  private final LeaderLatch leaderLatch;
  private final ReentrantLock agentLock;
  private final long agentLockTimeoutMs;
  private final AtomicInteger bootstrapStateNodeVersion = new AtomicInteger(0);

  @Inject
  public LifecycleHelper(BaragonWorkerDatastore workerDatastore,
                         BaragonAuthDatastore authDatastore,
                         BaragonAgentConfiguration configuration,
                         BaragonAgentMetadata baragonAgentMetadata,
                         FilesystemConfigHelper configHelper,
                         BaragonStateDatastore stateDatastore,
                         ServerProvider serverProvider,
                         AtomicReference<BaragonAgentState> agentState,
                         @Named(BaragonAgentServiceModule.BARAGON_AGENT_HTTP_CLIENT) HttpClient httpClient,
                         @Named(BaragonAgentServiceModule.AGENT_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                         @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.workerDatastore = workerDatastore;
    this.authDatastore = authDatastore;
    this.configuration = configuration;
    this.baragonAgentMetadata = baragonAgentMetadata;
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.serverProvider = serverProvider;
    this.agentState = agentState;
    this.httpClient = httpClient;
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public void notifyServiceWithRetry(final String action) {
    Callable<Void> callable = new Callable<Void>() {
      public Void call() throws Exception {
        notifyService(action);
        return null;
      }
    };

    Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
      .retryIfException()
      .withStopStrategy(StopStrategies.stopAfterAttempt(configuration.getMaxNotifyServiceAttempts()))
      .withWaitStrategy(WaitStrategies.exponentialWait(1, TimeUnit.SECONDS))
      .build();

    try {
      retryer.call(callable);
    } catch (Exception e) {
      if (action.equals("startup") && !configuration.isExitOnStartupError()) {
        LOG.error("Could not notify service of startup", e);
      } else {
        throw Throwables.propagate(e);
      }
    }
  }

  public void notifyService(String action) throws AgentStartupException {
    Collection<String> baseUris = workerDatastore.getBaseUris();
    if (!baseUris.isEmpty()) {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(String.format(SERVICE_CHECKIN_URL_FORMAT, baseUris.iterator().next(), configuration.getLoadBalancerConfiguration().getName(), action))
        .setMethod(HttpRequest.Method.POST)
        .setBody(baragonAgentMetadata);

      Map<String, BaragonAuthKey> authKeys = authDatastore.getAuthKeyMap();
      if (!authKeys.isEmpty()) {
        requestBuilder.setQueryParam("authkey").to(authKeys.entrySet().iterator().next().getValue().getValue());
      }

      HttpRequest request = requestBuilder.build();
      HttpResponse response = httpClient.execute(request);
      LOG.info(String.format("Got %s response from BaragonService", response.getStatusCode()));
      if (response.isError()) {
        throw new AgentStartupException(String.format("Bad response received from BaragonService %s", response.getAsString()));
      }
    }
  }

  public void writeStateFileIfConfigured() throws IOException {
    if (configuration.getStateFile().isPresent()) {
      LOG.info("Writing state file...");
      Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configuration.getStateFile().get()), "UTF-8"));
      try {
        writer.write("RUNNING");
      } finally {
        writer.close();
      }
    }
  }

  public boolean removeStateFile() {
    File stateFile = new File(configuration.getStateFile().get());
    return (!stateFile.exists() || stateFile.delete());
  }

  public void applyCurrentConfigs() throws AgentStartupException {
    LOG.info("Getting current state of the world from Baragon Service...");

    final Stopwatch stopwatch = Stopwatch.createStarted();
    final long now = System.currentTimeMillis();

    final Collection<String> services = stateDatastore.getServices();
    if (services.size() > 0) {
      ExecutorService executorService = Executors.newFixedThreadPool(services.size());
      List<Callable<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>>> todo = new ArrayList<>(services.size());

      Optional<Integer> maybeVersion = stateDatastore.getStateVersion();
      if (maybeVersion.isPresent()) {
        bootstrapStateNodeVersion.set(maybeVersion.get());
      }

      for (BaragonServiceState serviceState : getGlobalStateWithRetry()) {
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
        LOG.error("Caught exception while applying and parsing configs", e);
        if (configuration.isExitOnStartupError()) {
          Throwables.propagate(e);
        }
      }

      LOG.info("Applied {} services in {}ms", todo.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    } else {
      LOG.info("No services were found to apply");
    }
  }

  private Collection<BaragonServiceState> getGlobalStateWithRetry() throws AgentStartupException {
    Callable<Collection<BaragonServiceState>> callable = new Callable<Collection<BaragonServiceState>>() {
      public Collection<BaragonServiceState> call() throws Exception {
        return getGlobalState();
      }
    };

    Retryer<Collection<BaragonServiceState>> retryer = RetryerBuilder.<Collection<BaragonServiceState>>newBuilder()
        .retryIfException()
        .withStopStrategy(StopStrategies.stopAfterAttempt(configuration.getMaxGetGloablStateAttempts()))
        .withWaitStrategy(WaitStrategies.exponentialWait(1, TimeUnit.SECONDS))
        .build();

    try {
      return retryer.call(callable);
    } catch (Exception e) {
      LOG.error("Could not get global state from Baragon Service");
      throw Throwables.propagate(e);
    }
  }

  private Collection<BaragonServiceState> getGlobalState() throws AgentStartupException {
    Collection<String> baseUris = workerDatastore.getBaseUris();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(String.format(GLOBAL_STATE_FORMAT, baseUris.iterator().next()))
        .setMethod(Method.GET);

    Map<String, BaragonAuthKey> authKeys = authDatastore.getAuthKeyMap();
    if (!authKeys.isEmpty()) {
      requestBuilder.setQueryParam("authkey").to(authKeys.entrySet().iterator().next().getValue().getValue());
    }

    HttpRequest request = requestBuilder.build();
    HttpResponse response = httpClient.execute(request);
    LOG.info(String.format("Got %s response from BaragonService", response.getStatusCode()));
    if (response.isError()) {
      throw new AgentStartupException(String.format("Bad response received from BaragonService %s", response.getAsString()));
    }
    return response.getAs(new TypeReference<Collection<BaragonServiceState>>() {});
  }

  public void shutdown() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
    if (configuration.isDeregisterOnGracefulShutdown()) {
      LOG.info("Notifying BaragonService of shutdown...");
      notifyServiceWithRetry("shutdown");
    }
    if (configuration.getStateFile().isPresent()) {
      removeStateFile();
    }
  }

  public void checkStateNodeVersion() {
    agentState.set(BaragonAgentState.BOOTSTRAPING);
    try {
      Optional<Integer> maybeStateVersion = stateDatastore.getStateVersion();
      if (maybeStateVersion.isPresent()) {
        if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
          throw new LockTimeoutException("Could not acquire lock to reapply configs", agentLock);
        }
        try {
          if (bootstrapStateNodeVersion.get() < maybeStateVersion.get()) {
            applyCurrentConfigs();
            bootstrapStateNodeVersion.set(maybeStateVersion.get());
          }
        } catch (Exception e) {
          abort("Could not ensure configs are up to date, aborting", e);
        } finally {
          agentLock.unlock();
        }
      }
    } catch (Exception e) {
      abort("Interrupted while trying to reapply configs, shutting down", e);
    }
    agentState.set(BaragonAgentState.ACCEPTING);
  }

  @SuppressFBWarnings("DM_EXIT")
  public void abort(String message, Exception exception) {
    LOG.error(message, exception);
    flushLogs();
    Optional<Server> server = serverProvider.get();
    if (server.isPresent()) {
      try {
        server.get().stop();
        shutdown();
      } catch (Exception e) {
        LOG.warn("While aborting server", e);
      }
    } else {
      LOG.warn("Baragon Agent abort called before server has fully initialized!");
    }
    System.exit(1);
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

package com.hubspot.baragon.agent.lbs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;
import com.hubspot.baragon.exceptions.WorkerLimitReachedException;

@Singleton
public class LocalLbAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(LocalLbAdapter.class);

  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final ExecutorService destroyProcessExecutor;

  @Inject
  public LocalLbAdapter(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.destroyProcessExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("lb-shell-process-kill-%d").build());
  }

  private int executeWithTimeout(CommandLine command, int timeout) throws LbAdapterExecuteException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DefaultExecutor executor = new DefaultExecutor();
    executor.setStreamHandler(new PumpStreamHandler(baos));
    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

    // Start async and do our own time limiting instead of using a watchdog to avoid https://issues.apache.org/jira/browse/EXEC-62
    try {
      long start = System.currentTimeMillis();
      executor.execute(command, resultHandler);
      while (System.currentTimeMillis() - start < timeout && !resultHandler.hasResult()) {
        Thread.sleep(50);
      }
      if (resultHandler.hasResult()) {
        if (resultHandler.getException() != null) {
          throw resultHandler.getException();
        }
        return resultHandler.getExitValue();
      } else {
        CompletableFuture.runAsync(() -> executor.getWatchdog().destroyProcess(), destroyProcessExecutor);
        throw new LbAdapterExecuteException(baos.toString(Charsets.UTF_8.name()), command.toString());
      }
    } catch (ExecuteException|InterruptedException e) {
      throw new LbAdapterExecuteException(baos.toString(Charsets.UTF_8.name()), e, command.toString());
    }
  }

  private Optional<Integer> getOutputAsInt(String command) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      Process process = processBuilder.start();

      try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));) {
        List<String> output = new ArrayList<>();
        String line = br.readLine();
        while (line != null) {
          output.add(line);
          line = br.readLine();
        }
        return Optional.of(Integer.parseInt(output.get(0).trim()));
      } catch (Exception e) {
        LOG.error("Could not get worker count from command {}", command, e);
        return Optional.absent();
      }
    } catch (IOException ioe) {
      LOG.error("Could not get worker count from command {}", command, ioe);
      return Optional.absent();
    }
  }

  @Timed
  public void checkConfigs() throws InvalidConfigException {
    try {
      final long start = System.currentTimeMillis();
      final int exitCode = executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
      LOG.info("Checked configs via '{}' in {}ms (exit code = {})", loadBalancerConfiguration.getCheckConfigCommand(), System.currentTimeMillis() - start, exitCode);
    } catch (LbAdapterExecuteException e) {
      throw new InvalidConfigException(e.getOutput());
    } catch (IOException e) {
      throw new InvalidConfigException(e.getMessage());
    }
  }

  @Timed
  public void reloadConfigs() throws LbAdapterExecuteException, IOException, WorkerLimitReachedException {
    if (loadBalancerConfiguration.isLimitWorkerCount()) {
      if (loadBalancerConfiguration.getWorkerCountCommand().isPresent()) {
        checkWorkerCount();
      } else {
        LOG.warn("Asked to limit worker count, but no workerCountCommand was specified");
      }
    }
    final long start = System.currentTimeMillis();
    final int exitCode = executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
    LOG.info("Reloaded configs via '{}' in {}ms (exit code = {})", loadBalancerConfiguration.getReloadConfigCommand(), System.currentTimeMillis() - start, exitCode);
  }

  private void checkWorkerCount() throws WorkerLimitReachedException {
    Optional<Integer> workerCount = getOutputAsInt(loadBalancerConfiguration.getWorkerCountCommand().get());
    LOG.debug("Current worker count: {}", workerCount);
    if (!workerCount.isPresent() || workerCount.get() > loadBalancerConfiguration.getMaxLbWorkerCount()) {
      throw new WorkerLimitReachedException(String.format("%s LB workers currently running, wait for old workers to exit before attempting to reload configs", workerCount.get()));
    }
  }
}

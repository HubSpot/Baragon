package com.hubspot.baragon.agent.lbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;

@Singleton
public class LocalLbAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(LocalLbAdapter.class);

  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public LocalLbAdapter(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  private int executeWithTimeout(CommandLine command, int timeout) throws LbAdapterExecuteException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DefaultExecutor executor = new DefaultExecutor();

    executor.setStreamHandler(new PumpStreamHandler(baos));
    executor.setWatchdog(new ExecuteWatchdog(timeout));

    try {
      return executor.execute(command);
    } catch (ExecuteException e) {
      throw new LbAdapterExecuteException(baos.toString(Charsets.UTF_8.name()), e, command.toString());
    }
  }

  @Timed
  public void checkConfigs() throws InvalidConfigException {
    try {
      final long start = System.currentTimeMillis();
      final int exitCode = executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
      LOG.debug("Checked configs via '{}' in {}ms (exit code = {})", loadBalancerConfiguration.getCheckConfigCommand(), System.currentTimeMillis() - start, exitCode);
    } catch (LbAdapterExecuteException e) {
      throw new InvalidConfigException(e.getOutput());
    } catch (IOException e) {
      throw new InvalidConfigException(e.getMessage());
    }
  }

  @Timed
  public void reloadConfigs() throws LbAdapterExecuteException, IOException {
    final long start = System.currentTimeMillis();
    final int exitCode = executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
    LOG.debug("Reloaded configs via '{}' in {}ms (exit code = {})", loadBalancerConfiguration.getReloadConfigCommand(), System.currentTimeMillis() - start, exitCode);
  }
}

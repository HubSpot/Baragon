package com.hubspot.baragon.agent.lbs;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;

@Singleton
public class LocalLbAdapter {
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public LocalLbAdapter(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }
  
  private void executeWithTimeout(CommandLine command, int timeout) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DefaultExecutor executor = new DefaultExecutor();
    
    executor.setStreamHandler(new PumpStreamHandler(baos));
    executor.setWatchdog(new ExecuteWatchdog(timeout));
    
    try {
      executor.execute(command);
    } catch (ExecuteException e) {
      throw new RuntimeException(baos.toString(), e);
    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }

  public void checkConfigs() throws InvalidConfigException {
    try {
      executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
    } catch (RuntimeException e) {
      throw new InvalidConfigException(e.getMessage());
    }
  }

  public void reloadConfigs() {
    executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
  }
}

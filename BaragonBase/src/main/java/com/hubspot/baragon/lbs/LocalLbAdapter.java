package com.hubspot.baragon.lbs;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;

public class LocalLbAdapter implements LbAdapter {
  public static int COMMAND_TIMEOUT = 10000;

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
  
  @Override
  public void checkConfigs() throws InvalidConfigException {
    try {
      executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), COMMAND_TIMEOUT);
    } catch (RuntimeException e) {
      throw new InvalidConfigException(e.getMessage());
    }
  }

  @Override
  public void reloadConfigs() {
    executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), COMMAND_TIMEOUT);
  }
}

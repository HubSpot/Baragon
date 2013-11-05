package com.hubspot.baragon.lbs;

import java.io.ByteArrayOutputStream;

import com.google.inject.Inject;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.common.base.Throwables;

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
  public void checkConfigs() {
    executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), COMMAND_TIMEOUT);
  }

  @Override
  public void reloadConfigs() {
    executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), COMMAND_TIMEOUT);
  }
}

package com.hubspot.baragon.lbs;

import java.io.ByteArrayOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.common.base.Throwables;

public abstract class LocalLbAdapter implements LbAdapter {
  public static int COMMAND_TIMEOUT = 10000;
  
  protected abstract CommandLine getCheckConfigCommand();
  protected abstract CommandLine getReloadConfigCommand();
  
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
    executeWithTimeout(getCheckConfigCommand(), COMMAND_TIMEOUT);
  }

  @Override
  public void reloadConfigs() {
    executeWithTimeout(getReloadConfigCommand(), COMMAND_TIMEOUT);
  }
}

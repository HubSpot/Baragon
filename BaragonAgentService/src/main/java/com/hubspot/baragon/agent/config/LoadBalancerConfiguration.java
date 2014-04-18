package com.hubspot.baragon.agent.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class LoadBalancerConfiguration {

  @NotNull
  private String name;

  @NotNull
  private String rootPath;

  @NotNull
  private String checkConfigCommand;

  @NotNull
  private String reloadConfigCommand;

  @Min(0)
  private int commandTimeoutMs = 10000;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getCheckConfigCommand() {
    return checkConfigCommand;
  }

  public void setCheckConfigCommand(String checkConfigCommand) {
    this.checkConfigCommand = checkConfigCommand;
  }

  public String getReloadConfigCommand() {
    return reloadConfigCommand;
  }

  public void setReloadConfigCommand(String reloadConfigCommand) {
    this.reloadConfigCommand = reloadConfigCommand;
  }

  public int getCommandTimeoutMs() {
    return commandTimeoutMs;
  }

  public void setCommandTimeoutMs(int commandTimeoutMs) {
    this.commandTimeoutMs = commandTimeoutMs;
  }
}

package com.hubspot.baragon.agent.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class LoadBalancerConfiguration {
  public static final int DEFAULT_COMMAND_TIMEOUT_MS = 10000;

  @NotNull
  private String name;

  private String domain;

  @NotNull
  private String rootPath;

  @NotNull
  private String checkConfigCommand;

  @NotNull
  private String reloadConfigCommand;

  @Min(0)
  private int commandTimeoutMs = DEFAULT_COMMAND_TIMEOUT_MS;

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

  public Optional<String> getDomain() {
    return Optional.fromNullable(Strings.emptyToNull(domain));
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }
}

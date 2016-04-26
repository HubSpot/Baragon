package com.hubspot.baragon.agent.config;

import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class LoadBalancerConfiguration {
  public static final int DEFAULT_COMMAND_TIMEOUT_MS = 10000;

  @NotNull
  private String name;

  private String defaultDomain;

  @Deprecated
  private String domain;

  @NotNull
  private String rootPath;

  @NotNull
  private String checkConfigCommand;

  @NotNull
  private String reloadConfigCommand;

  @Min(0)
  private int commandTimeoutMs = DEFAULT_COMMAND_TIMEOUT_MS;

  @NotNull
  private Set<String> domains = Collections.emptySet();

  @NotNull
  @Min(1)
  private int maxLbWorkerCount = 1;

  @NotNull
  private Optional<String> workerCountCommand = Optional.absent();

  private boolean limitWorkerCount = false;

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

  public Optional<String> getDefaultDomain() {
    return Optional.fromNullable(Strings.emptyToNull(defaultDomain)).or(Optional.fromNullable(Strings.emptyToNull(domain)));
  }

  public void setDefaultDomain(String defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  @Deprecated
  public Optional<String> getDomain() {
    return getDefaultDomain();
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Set<String> getDomains() {
    return domains;
  }

  public void setDomains(Set<String> domains) {
    this.domains = domains;
  }

  public int getMaxLbWorkerCount() {
    return maxLbWorkerCount;
  }

  public void setMaxLbWorkerCount(int maxLbWorkerCount) {
    this.maxLbWorkerCount = maxLbWorkerCount;
  }

  public Optional<String> getWorkerCountCommand() {
    return workerCountCommand;
  }

  public void setWorkerCountCommand(Optional<String> workerCountCommand) {
    this.workerCountCommand = workerCountCommand;
  }

  public boolean isLimitWorkerCount() {
    return limitWorkerCount;
  }

  public void setLimitWorkerCount(boolean limitWorkerCount) {
    this.limitWorkerCount = limitWorkerCount;
  }
}

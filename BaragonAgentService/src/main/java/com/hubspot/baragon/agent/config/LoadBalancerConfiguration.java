package com.hubspot.baragon.agent.config;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class LoadBalancerConfiguration {
  public static final int DEFAULT_COMMAND_TIMEOUT_MS = 10000;

  @NotNull
  private String name;

  @Deprecated
  private String domain;

  private String defaultDomain;

  @NotNull
  private String rootPath;

  @NotNull
  private String checkConfigCommand;

  @NotNull
  private String reloadConfigCommand;

  @Min(0)
  private int commandTimeoutMs = DEFAULT_COMMAND_TIMEOUT_MS;

  @NotNull
  private List<String> domainsServed = Collections.emptyList();

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

  @Deprecated
  public Optional<String> getDomain() {
    return getDefaultDomain();
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Optional<String> getDefaultDomain() {
    return Optional.fromNullable(Strings.emptyToNull(defaultDomain)).or(Optional.fromNullable(Strings.emptyToNull(domain)));
  }

  public void setDefaultDomain(String defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  public List<String> getDomainsServed() {
    return domainsServed;
  }

  public void setDomainsServed(List<String> domainsServed) {
    this.domainsServed = domainsServed;
  }
}

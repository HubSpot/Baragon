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

  @NotNull
  private String proxyTemplate;

  @NotNull
  private String upstreamTemplate;

  @NotNull
  private Boolean rollbackConfigsIfInvalid = false;

  @NotNull
  private Boolean alwaysApplyConfigs = false;

  @Min(0)
  private int commandTimeoutMs = 10000;

  public String getName() {
    return name;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public void setCheckConfigCommand(String checkConfigCommand) {
    this.checkConfigCommand = checkConfigCommand;
  }

  public String getCheckConfigCommand() {
    return checkConfigCommand;
  }

  public void setReloadConfigCommand(String reloadConfigCommand) {
    this.reloadConfigCommand = reloadConfigCommand;
  }

  public String getReloadConfigCommand() {
    return reloadConfigCommand;
  }

  public void setProxyTemplate(String proxyTemplate) {
    this.proxyTemplate = proxyTemplate;
  }

  public String getProxyTemplate() {
    return proxyTemplate;
  }

  public void setUpstreamTemplatePath(String upstreamTemplate) {
    this.upstreamTemplate = upstreamTemplate;
  }

  public String getUpstreamTemplate() {
    return upstreamTemplate;
  }

  public void setRollbackConfigsIfInvalid(Boolean rollbackConfigsIfInvalid) {
    this.rollbackConfigsIfInvalid = rollbackConfigsIfInvalid;
  }

  public Boolean getRollbackConfigsIfInvalid() {
    return rollbackConfigsIfInvalid;
  }

  public Boolean getAlwaysApplyConfigs() {
    return alwaysApplyConfigs;
  }

  public void setAlwaysApplyConfigs(Boolean alwaysApplyConfigs) {
    this.alwaysApplyConfigs = alwaysApplyConfigs;
  }

  public void setUpstreamTemplate(String upstreamTemplate) {
    this.upstreamTemplate = upstreamTemplate;
  }

  public int getCommandTimeoutMs() {
    return commandTimeoutMs;
  }

  public void setCommandTimeoutMs(int commandTimeoutMs) {
    this.commandTimeoutMs = commandTimeoutMs;
  }
}

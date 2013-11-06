package com.hubspot.baragon.config;

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
}

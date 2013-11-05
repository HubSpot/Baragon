package com.hubspot.baragon.config;

import org.apache.commons.exec.CommandLine;

public class LoadBalancerConfiguration {
  private String domain;
  private String type;
  private String name;
  private String rootPath;
  private String checkConfigCommand;
  private String reloadConfigCommand;

  public String getDomain() {
    return domain;
  }

  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setType(String type) {
    this.type = type;
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
}

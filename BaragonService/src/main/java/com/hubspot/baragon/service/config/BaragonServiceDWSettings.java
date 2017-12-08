package com.hubspot.baragon.service.config;

public class BaragonServiceDWSettings {
  private final int port;
  private final String contextPath;

  public BaragonServiceDWSettings(int port, String contextPath) {
    this.port = port;
    this.contextPath = contextPath;
  }

  public int getPort() {
    return port;
  }

  public String getContextPath() {
    return contextPath;
  }
}

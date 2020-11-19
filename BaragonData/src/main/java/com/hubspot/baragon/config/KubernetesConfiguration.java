package com.hubspot.baragon.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.ImmutableList;

public class KubernetesConfiguration {
  private boolean enabled = false;

  // Client Configuration
  private int maxConcurrentRequests = 64;
  private int maxConcurrentRequestsPerHost = 10;
  private int connectTimeoutMillis = 5000;
  private int requestTimeoutMillis = 30000;
  private int websocketTimeoutMillis = 5000;
  private int websocketPingIntervalMillis = 5000;
  private int loggingIntervalMillis = 900000;
  private int watchReconnectIntervalMillis = 1000;
  private int watchReconnectLimit = -1;
  private String masterUrl;
  private String token;

  // Annotations + Labels
  private String basePathAnnotation = "baragon.hubspot.com/base-path";
  private Map<String, String> baragonLabelFilter = Collections.emptyMap();
  private String serviceNameLabel = "baragon.hubspot.com/service-name";
  private String upstreamGroupsLabel = "baragon.hubspot.com/upstream-group";
  private String lbGroupsAnnotation = "baragon.hubspot.com/lb-groups";
  private String domainsAnnotation = "baragon.hubspot.com/domains";
  private String customConfigAnnotation = "baragon.hubspot.com/customConfig";
  private String ownersAnnotation = "baragon.hubspot.com/owners";
  private String templateNameAnnotation = "baragon.hubspot.com/template-name";
  private String protocolLabel = "baragon.hubspot.com/desiredProtocol";
  private String additionalPathsAnnotation = "baragon.hubspot.com/additionalPaths";

  @NotEmpty
  private List<String> ignoreUpstreamGroups = ImmutableList.of();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxConcurrentRequests() {
    return maxConcurrentRequests;
  }

  public void setMaxConcurrentRequests(int maxConcurrentRequests) {
    this.maxConcurrentRequests = maxConcurrentRequests;
  }

  public int getMaxConcurrentRequestsPerHost() {
    return maxConcurrentRequestsPerHost;
  }

  public void setMaxConcurrentRequestsPerHost(int maxConcurrentRequestsPerHost) {
    this.maxConcurrentRequestsPerHost = maxConcurrentRequestsPerHost;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public void setConnectTimeoutMillis(int connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  public int getRequestTimeoutMillis() {
    return requestTimeoutMillis;
  }

  public void setRequestTimeoutMillis(int requestTimeoutMillis) {
    this.requestTimeoutMillis = requestTimeoutMillis;
  }

  public int getWebsocketTimeoutMillis() {
    return websocketTimeoutMillis;
  }

  public void setWebsocketTimeoutMillis(int websocketTimeoutMillis) {
    this.websocketTimeoutMillis = websocketTimeoutMillis;
  }

  public int getWebsocketPingIntervalMillis() {
    return websocketPingIntervalMillis;
  }

  public void setWebsocketPingIntervalMillis(int websocketPingIntervalMillis) {
    this.websocketPingIntervalMillis = websocketPingIntervalMillis;
  }

  public int getLoggingIntervalMillis() {
    return loggingIntervalMillis;
  }

  public void setLoggingIntervalMillis(int loggingIntervalMillis) {
    this.loggingIntervalMillis = loggingIntervalMillis;
  }

  public int getWatchReconnectIntervalMillis() {
    return watchReconnectIntervalMillis;
  }

  public void setWatchReconnectIntervalMillis(int watchReconnectIntervalMillis) {
    this.watchReconnectIntervalMillis = watchReconnectIntervalMillis;
  }

  public int getWatchReconnectLimit() {
    return watchReconnectLimit;
  }

  public void setWatchReconnectLimit(int watchReconnectLimit) {
    this.watchReconnectLimit = watchReconnectLimit;
  }

  public String getMasterUrl() {
    return masterUrl;
  }

  public void setMasterUrl(String masterUrl) {
    this.masterUrl = masterUrl;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getBasePathAnnotation() {
    return basePathAnnotation;
  }

  public void setBasePathAnnotation(String basePathAnnotation) {
    this.basePathAnnotation = basePathAnnotation;
  }

  public Map<String, String> getBaragonLabelFilter() {
    return baragonLabelFilter;
  }

  public void setBaragonLabelFilter(Map<String, String> baragonLabelFilter) {
    this.baragonLabelFilter = baragonLabelFilter;
  }

  public String getServiceNameLabel() {
    return serviceNameLabel;
  }

  public void setServiceNameLabel(String serviceNameLabel) {
    this.serviceNameLabel = serviceNameLabel;
  }

  public String getUpstreamGroupsLabel() {
    return upstreamGroupsLabel;
  }

  public void setUpstreamGroupsLabel(String upstreamGroupsLabel) {
    this.upstreamGroupsLabel = upstreamGroupsLabel;
  }

  public List<String> getIgnoreUpstreamGroups() {
    return ignoreUpstreamGroups;
  }

  public String[] getIgnoreUpstreamGroupsAsArray() {
    return (String[]) ignoreUpstreamGroups.toArray();
  }

  public void setIgnoreUpstreamGroups(List<String> upstreamGroups) {
    this.ignoreUpstreamGroups = upstreamGroups;
  }

  public String getLbGroupsAnnotation() {
    return lbGroupsAnnotation;
  }

  public void setLbGroupsAnnotation(String lbGroupsAnnotation) {
    this.lbGroupsAnnotation = lbGroupsAnnotation;
  }

  public String getDomainsAnnotation() {
    return domainsAnnotation;
  }

  public void setDomainsAnnotation(String domainsAnnotation) {
    this.domainsAnnotation = domainsAnnotation;
  }

  public String getCustomConfigAnnotation() {
    return customConfigAnnotation;
  }

  public void setCustomConfigAnnotation(String customConfigAnnotation) {
    this.customConfigAnnotation = customConfigAnnotation;
  }

  public String getOwnersAnnotation() {
    return ownersAnnotation;
  }

  public void setOwnersAnnotation(String ownersAnnotation) {
    this.ownersAnnotation = ownersAnnotation;
  }

  public String getTemplateNameAnnotation() {
    return templateNameAnnotation;
  }

  public void setTemplateNameAnnotation(String templateNameAnnotation) {
    this.templateNameAnnotation = templateNameAnnotation;
  }

  public String getProtocolLabel() {
    return protocolLabel;
  }

  public void setProtocolLabel(String protocolLabel) {
    this.protocolLabel = protocolLabel;
  }

  public String getAdditionalPathsAnnotation() {
    return additionalPathsAnnotation;
  }

  public void setAdditionalPathsAnnotation(String additionalPathsAnnotation) {
    this.additionalPathsAnnotation = additionalPathsAnnotation;
  }
}

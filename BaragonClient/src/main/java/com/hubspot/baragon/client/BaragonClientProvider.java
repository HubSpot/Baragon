package com.hubspot.baragon.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.horizon.HttpClient;

@Singleton
public class BaragonClientProvider implements Provider<BaragonClient> {
  private static final String DEFAULT_CONTEXT_PATH = "baragon/v2";

  private final HttpClient httpClient;

  private String contextPath = DEFAULT_CONTEXT_PATH;
  private List<String> hosts = Collections.emptyList();

  @Inject
  public BaragonClientProvider(@Named(BaragonClientModule.HTTP_CLIENT_NAME) HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Inject(optional=true) // optional because we have a default
  public BaragonClientProvider setContextPath(@Named(BaragonClientModule.CONTEXT_PATH) String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Inject(optional=true) // optional in case we use fixed hosts
  public BaragonClientProvider setHosts(@Named(BaragonClientModule.HOSTS_PROPERTY_NAME) String commaSeparatedHosts) {
    return setHosts(commaSeparatedHosts.split(","));
  }

  @Inject(optional=true)
  public BaragonClientProvider setHosts(@Named(BaragonClientModule.HOSTS_PROPERTY_NAME) List<String> hosts) {
    this.hosts = ImmutableList.copyOf(hosts);
    return this;
  }

  public BaragonClientProvider setHosts(String... hosts) {
    this.hosts = Arrays.asList(hosts);
    return this;
  }

  @Override
  public BaragonClient get() {
    Preconditions.checkState(contextPath != null, "contextPath null");
    Preconditions.checkState(!hosts.isEmpty(), "no hosts provided");
    return new BaragonClient(contextPath, httpClient, hosts);
  }
}

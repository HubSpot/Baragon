package com.hubspot.baragon.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.horizon.HttpClient;

@Singleton
public class BaragonClientProvider implements Provider<BaragonServiceClient> {
  private static final String DEFAULT_CONTEXT_PATH = "baragon/v2";

  private final HttpClient httpClient;

  private String contextPath = DEFAULT_CONTEXT_PATH;
  private List<String> hosts = Collections.emptyList();
  private Provider<List<String>> hostsProvider = null;
  private Optional<String> authkey = Optional.absent();
  private Provider<Optional<String>> authkeyProvider = null;

  @Inject
  public BaragonClientProvider(@Named(BaragonClientModule.HTTP_CLIENT_NAME) HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Inject(optional=true) // optional because we have a default
  public BaragonClientProvider setContextPath(@Named(BaragonClientModule.CONTEXT_PATH_PROPERTY_NAME) String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Inject(optional=true) // optional in case we use fixed hosts
  public BaragonClientProvider setHosts(@Named(BaragonClientModule.HOSTS_PROPERTY_NAME) String commaSeparatedHosts) {
    return setHosts(commaSeparatedHosts.split(","));
  }

  @Inject(optional=true)
  public BaragonClientProvider setHosts(@Named(BaragonClientModule.HOSTS_PROPERTY_NAME) List<String> hosts) {
    mapAndSetHosts(hosts);
    return this;
  }

  @Inject(optional=true)
  public BaragonClientProvider setAuthkey(@Named(BaragonClientModule.AUTHKEY_PROPERTY_NAME) Optional<String> authkey) {
    this.authkey = authkey;
    return this;
  }

  public BaragonClientProvider setHosts(String... hosts) {
    mapAndSetHosts(Arrays.asList(hosts));
    return this;
  }

  private void mapAndSetHosts(List<String> hosts) {
    this.hosts = ImmutableList.copyOf(hosts.stream().map((h) -> h.startsWith("http") ? h : String.format("http://%s", h)).collect(Collectors.toList()));
  }

  @Inject(optional = true)
  public BaragonClientProvider setHostsProvider(Provider<List<String>> hostsProvider) {
    this.hostsProvider = hostsProvider;
    return this;
  }

  @Inject(optional = true)
  public BaragonClientProvider setAuthkeyProvider(Provider<Optional<String>> authkeyProvider) {
    this.authkeyProvider = authkeyProvider;
    return this;
  }

  @Override
  public BaragonServiceClient get() {
    Preconditions.checkState(contextPath != null, "contextPath null");
    Preconditions.checkState(!hosts.isEmpty() || hostsProvider != null, "no hosts provided");
    Preconditions.checkState(authkey != null, "authkey null");

    if (authkeyProvider == null) {
      authkeyProvider = ProviderUtils.of(authkey);
    }
    if (hostsProvider == null) {
      hostsProvider = ProviderUtils.of(hosts);
    }

    return new BaragonServiceClient(contextPath, httpClient, hostsProvider, authkeyProvider);
  }
}

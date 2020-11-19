package com.hubspot.baragon.kubernetes;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.kubernetes.client.utils.Serialization;

public class KubernetesWatcherModule implements Module {
  public static final String BARAGON_KUBERNETES_CLIENT = "baragon.kubernetes.client";

  @Override
  public void configure(Binder binder) {
    binder.bind(KubernetesEndpointsWatcher.class).in(Scopes.SINGLETON);
    binder.bind(KubernetesServiceWatcher.class).in(Scopes.SINGLETON);

    Serialization.jsonMapper().registerModules(new GuavaModule(), new Jdk8Module());
    PatchUtils.patchMapper().registerModules(new GuavaModule(), new Jdk8Module());
  }

  @Provides
  @Singleton
  @Named(BARAGON_KUBERNETES_CLIENT)
  public KubernetesClient provideKubernetesClient(KubernetesConfiguration kubernetesConfiguration) {
    Config config = new ConfigBuilder().build();
    config.setMasterUrl(kubernetesConfiguration.getMasterUrl());
    config.setOauthToken(kubernetesConfiguration.getToken());
    config.setMaxConcurrentRequests(kubernetesConfiguration.getMaxConcurrentRequests());
    config.setMaxConcurrentRequestsPerHost(kubernetesConfiguration.getMaxConcurrentRequestsPerHost());
    config.setConnectionTimeout((int) kubernetesConfiguration.getConnectTimeoutMillis());
    config.setRequestTimeout((int) kubernetesConfiguration.getRequestTimeoutMillis());
    config.setWebsocketTimeout((int) kubernetesConfiguration.getWebsocketTimeoutMillis());
    config.setWebsocketPingInterval((int) kubernetesConfiguration.getWebsocketPingIntervalMillis());
    config.setLoggingInterval((int) kubernetesConfiguration.getLoggingIntervalMillis());
    config.setWatchReconnectInterval((int) kubernetesConfiguration.getWatchReconnectIntervalMillis());
    config.setWatchReconnectLimit(kubernetesConfiguration.getWatchReconnectLimit());
    config.setUserAgent("Baragon");
    return new DefaultKubernetesClient(config);
  }
}

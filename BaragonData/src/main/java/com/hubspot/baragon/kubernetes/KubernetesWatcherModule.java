package com.hubspot.baragon.kubernetes;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;

import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesWatcherModule implements Module {
  public static final String BARAGON_KUBERNETES_CLIENT = "baragon.kubernetes.client";

  @Override
  public void configure(Binder binder) {
    binder.bind(KubernetesWatcher.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  @Named(BARAGON_KUBERNETES_CLIENT)
  public KubernetesClient provideKubernetesClient(KubernetesConfiguration kubernetesConfiguration) {
    // TODO
    return null;
  }
}

package com.hubspot.baragon.kubernetes;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.kubernetes.listeners.DeploymentAddedListener;
import com.hubspot.baragon.kubernetes.listeners.KubernetesEventListener;

public class BaragonKubernetesIntegrationModule extends AbstractModule {
  public static final String EVENT_LISTENERS = "kubernetes.event.listeners";

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  @Named(EVENT_LISTENERS)
  public Map<KubernetesEvent, KubernetesEventListener> provideEventListeners() {
    Map<KubernetesEvent, KubernetesEventListener> listeners = new HashMap<>();
    listeners.put(KubernetesEvent.DEPLOYMENT_ADDED, new DeploymentAddedListener());

    if (listeners.size() != KubernetesEvent.values().length) {
      throw new RuntimeException("Missing KubernetesEvents/KubernetesEventListeners mappings.");
    }
    return listeners;
  }
}

package com.hubspot.baragon.kubernetes;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.baragon.kubernetes.listeners.EndpointsAddedOrModifiedListener;
import com.hubspot.baragon.kubernetes.listeners.EndpointsDeletedListener;
import com.hubspot.baragon.kubernetes.listeners.KubernetesEventListener;
import com.hubspot.baragon.service.config.BaragonConfiguration;

public class BaragonKubernetesIntegrationModule extends AbstractModule {
  public static final String EVENT_LISTENERS = "kubernetes.event.listeners";

  private final BaragonConfiguration baragonConfiguration;

  public BaragonKubernetesIntegrationModule(BaragonConfiguration baragonConfiguration) {
    this.baragonConfiguration = baragonConfiguration;
  }

  @Override
  protected void configure() {
    if (baragonConfiguration.isKubernetesIntegrated()) {
      bind(BaragonKubernetesManaged.class);
    }
  }

  @Provides
  @Singleton
  @Named(EVENT_LISTENERS)
  public Map<KubernetesEvent, KubernetesEventListener> provideEventListeners(BaragonServiceClient baragonClient) {
    Map<KubernetesEvent, KubernetesEventListener> listeners = new HashMap<>();

    EndpointsAddedOrModifiedListener endpointsAddedListener = new EndpointsAddedOrModifiedListener(baragonClient, baragonConfiguration);
    listeners.put(KubernetesEvent.ENDPOINTS_ADDED, endpointsAddedListener);
    listeners.put(KubernetesEvent.ENDPOINTS_MODIFIED, endpointsAddedListener);
    listeners.put(KubernetesEvent.ENDPOINTS_DELETED, new EndpointsDeletedListener(baragonConfiguration));

    if (listeners.size() != KubernetesEvent.values().length) {
      throw new RuntimeException("Missing KubernetesEvents/KubernetesEventListeners mappings.");
    }
    return listeners;
  }
}

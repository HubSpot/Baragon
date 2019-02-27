package com.hubspot.baragon.kubernetes.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.KubernetesIntegrationConfiguration;

import io.fabric8.kubernetes.api.model.Endpoints;

public class EndpointsDeletedListener implements KubernetesEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(EndpointsDeletedListener.class);

  private final KubernetesIntegrationConfiguration kubesConfig;

  public EndpointsDeletedListener(BaragonConfiguration baragonConfiguration) {
    this.kubesConfig = baragonConfiguration.getKubernetesIntegrationConfiguration().get();
  }

  public void handleEvent(Endpoints endpoints) {
    // TODO

  }
}

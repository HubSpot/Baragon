package com.hubspot.baragon.kubernetes.listeners;

import io.fabric8.kubernetes.api.model.Endpoints;

public interface KubernetesEventListener {
  void handleEvent(Endpoints endpoints);
}

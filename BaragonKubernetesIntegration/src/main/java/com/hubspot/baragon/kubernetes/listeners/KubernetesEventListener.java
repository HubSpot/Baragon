package com.hubspot.baragon.kubernetes.listeners;

import io.fabric8.kubernetes.api.model.Event;

public interface KubernetesEventListener {
  void handleEvent(Event event);
}

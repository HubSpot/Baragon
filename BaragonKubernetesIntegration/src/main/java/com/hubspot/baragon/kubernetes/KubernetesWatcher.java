package com.hubspot.baragon.kubernetes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.kubernetes.listeners.KubernetesEventListener;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

public class KubernetesWatcher implements Watcher<Event> {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesWatcher.class);

  private final KubernetesClient client;
  private final ExecutorService executorService;
  private final Map<KubernetesEvent, KubernetesEventListener> eventListeners;

  @Inject
  KubernetesWatcher(@Named(BaragonKubernetesIntegrationModule.EVENT_LISTENERS) Map<KubernetesEvent, KubernetesEventListener> eventListeners) {
    this.client = new DefaultKubernetesClient();
    this.executorService = Executors.newCachedThreadPool();
    this.eventListeners = eventListeners;
  }

  public void run() {
    client.events().inAnyNamespace().watch(this);
  }

  @Override
  public void eventReceived(Action action, Event event) {
    getEventType(action, event).ifPresent(kubernetesEvent ->
        executorService.submit(() -> eventListeners.get(kubernetesEvent).handleEvent(event)));
  }

  private Optional<KubernetesEvent> getEventType(Action action, Event event) {
    String kind = event.getInvolvedObject().getKind();

    switch (action) {
      case ADDED:
        if (kind.equals("Deployment")) {
          return Optional.of(KubernetesEvent.DEPLOYMENT_ADDED);
        }
        break;
      case DELETED:
        if (kind.equals("Deployment")) {
          return Optional.of(KubernetesEvent.DEPLOYMENT_ADDED);
        }
      default:
        LOG.warn("Ignoring event where {} {}", kind, action.name());
    }
    return Optional.empty();
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    LOG.info("Watcher close due to " + cause);
  }

}

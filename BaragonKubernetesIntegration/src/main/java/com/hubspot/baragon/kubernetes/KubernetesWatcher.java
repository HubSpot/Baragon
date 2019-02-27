package com.hubspot.baragon.kubernetes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.kubernetes.listeners.KubernetesEventListener;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

public class KubernetesWatcher implements Watcher<Endpoints> {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesWatcher.class);

  private static final String TEST_CLUSTER_URL = "https://k8s-iad03-test-api.iad03.hubinfraqa.com";
  private final KubernetesClient client;
  private final ExecutorService executorService;
  private final Map<KubernetesEvent, KubernetesEventListener> eventListeners;

  @Inject
  KubernetesWatcher(@Named(BaragonKubernetesIntegrationModule.EVENT_LISTENERS) Map<KubernetesEvent, KubernetesEventListener> eventListeners) {
    Config config = new ConfigBuilder()
        .withMasterUrl(TEST_CLUSTER_URL)
        .build();

    this.client = new DefaultKubernetesClient(config);
    this.executorService = Executors.newCachedThreadPool();
    this.eventListeners = eventListeners;
  }

  public void run() {
    LOG.info("Master URL: {}", client.getMasterUrl());
    LOG.info("Namespaces: {}", client.namespaces().list().getItems().stream().map(x -> x.getMetadata().getName()).collect(Collectors.toList()));
    client.endpoints().inNamespace("paas-run-playground").watch(this);
  }


  @Override public void eventReceived(Action action, Endpoints endpoints) {
    // TODO: Ignore ADDED endpoints on startup
    LOG.info("Got {}: {}", action, endpoints);

//    getEventType(action, endpoints).ifPresent(kubernetesEvent ->
//        executorService.submit(() -> eventListeners.get(kubernetesEvent).handleEvent(endpoints)));
  }

  private Optional<KubernetesEvent> getEventType(Action action, Endpoints endpoints) {
    String kind = endpoints.getKind();

    switch (action) {
      case ADDED:
        if (kind.equals("Deployment")) {
          return Optional.of(KubernetesEvent.ENDPOINTS_ADDED);
        }
        break;
      case DELETED:
        if (kind.equals("Deployment")) {
          return Optional.of(KubernetesEvent.ENDPOINTS_DELETED);
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

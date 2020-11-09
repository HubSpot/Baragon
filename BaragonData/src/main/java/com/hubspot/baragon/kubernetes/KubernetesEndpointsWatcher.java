package com.hubspot.baragon.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.models.UpstreamInfo;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;

@Singleton
public class KubernetesEndpointsWatcher extends BaragonKubernetesWatcher<Endpoints> {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesEndpointsWatcher.class);

  private final KubernetesListener listener;
  private final KubernetesConfiguration kubernetesConfiguration;
  private final KubernetesClient kubernetesClient;

  @Inject
  public KubernetesEndpointsWatcher(KubernetesListener listener,
                                    KubernetesConfiguration kubernetesConfiguration,
                                    @Named(KubernetesWatcherModule.BARAGON_KUBERNETES_CLIENT) KubernetesClient kubernetesClient) {
    super();
    this.listener = listener;
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.kubernetesClient = kubernetesClient;
  }

  public void createWatch(boolean processInitialFetch) {
    this.processInitialFetch = processInitialFetch;
    EndpointsList list = kubernetesClient.endpoints()
        .inAnyNamespace()
        .withLabels(kubernetesConfiguration.getBaragonLabelFilter())
        .withLabel(kubernetesConfiguration.getServiceNameLabel())
        .list();

    if (processInitialFetch) {
      try {
        LOG.info("Got {} initial endpoints from k8s", list.getItems().size());
        list.getItems()
            .forEach(this::processUpdatedEndpoint);
      } catch (Throwable t) {
        LOG.error("Exception processing initial endpoints, still attempting to create watcher", t);
      }
    }

    watch = kubernetesClient.endpoints()
        .inAnyNamespace()
        .withLabels(kubernetesConfiguration.getBaragonLabelFilter())
        .withLabel(kubernetesConfiguration.getServiceNameLabel())
        .withResourceVersion(list.getMetadata().getResourceVersion())
        .watch(this);
  }

  @Override
  public void eventReceived(Action action, Endpoints endpoints) {
    LOG.info("Received {} update from k8s for {}", action, endpoints.getMetadata().getName());
    switch (action) {
      case ADDED:
      case MODIFIED:
        processUpdatedEndpoint(endpoints);
        break;
      case DELETED:
        processEndpointDelete(endpoints);
        break;
      case ERROR:
      default:
        LOG.warn("No handling for action: {} (endpoints: {})", action, endpoints);
    }
  }

  private void processEndpointDelete(Endpoints endpoints) {
    String serviceName = getServiceName(endpoints);
    if (serviceName == null) {
      return;
    }
    Map<String, String> labels = endpoints.getMetadata().getLabels();
    String upstreamGroup = labels.getOrDefault(kubernetesConfiguration.getUpstreamGroupsLabel(), "default");
    if (kubernetesConfiguration.getIgnoreUpstreamGroups().contains(upstreamGroup)) {
      LOG.warn("Upstream group not managed by baragon, skipping (endpoints: {})", endpoints);
      return;
    }
    listener.processEndpointsDelete(serviceName, upstreamGroup);
  }

  private void processUpdatedEndpoint(Endpoints endpoints) {
    String serviceName = getServiceName(endpoints);
    if (serviceName == null) {
      return;
    }

    Map<String, String> labels = endpoints.getMetadata().getLabels();
    String upstreamGroup = labels.getOrDefault(kubernetesConfiguration.getUpstreamGroupsLabel(), "default");
    if (kubernetesConfiguration.getIgnoreUpstreamGroups().contains(upstreamGroup)) {
      LOG.warn("Upstream group not managed by baragon, skipping (endpoints: {})", endpoints);
      return;
    }

    String desiredProtocol = Optional.fromNullable(labels.get(kubernetesConfiguration.getProtocolLabel())).or("HTTP");
    listener.processUpstreamsUpdate(serviceName, upstreamGroup, parseActiveUpstreams(endpoints, upstreamGroup, desiredProtocol));
  }

  private String getServiceName(Endpoints endpoints) {
    Map<String, String> labels = endpoints.getMetadata().getLabels();
    if (labels == null) {
      LOG.warn("No labels present on endpoint {}", endpoints.getMetadata().getName());
      return null;
    }
    String serviceName = labels.get(kubernetesConfiguration.getServiceNameLabel());
    if (serviceName == null) {
      LOG.warn("Could not get service name for endpoint update (endpoints: {})", endpoints);
      return null;
    }
    return serviceName;
  }

  // TODO - how should we handle which port to use when there are multiple? Name? Protocol?
  private List<UpstreamInfo> parseActiveUpstreams(Endpoints endpoints, String upstreamGroup, String protocol) {
    List<UpstreamInfo> upstreams = new ArrayList<>();
    for (EndpointSubset subset : endpoints.getSubsets()) {
      Integer port = getPort(subset, protocol);
      for (EndpointAddress address : subset.getAddresses()) {
        upstreams.add(new UpstreamInfo(
            String.format("%s:%d", address.getIp(), port),
            Optional.absent(),
            Optional.absent(),
            Optional.of(upstreamGroup)
        ));
      }
    }
    return upstreams;
  }

  private Integer getPort(EndpointSubset subset, String protocol) {
    for (EndpointPort port : subset.getPorts()) {
      if (port.getProtocol().equals(protocol)) {
        return port.getPort();
      }
    }
    // Return the first thing we can find if the desired protocol isn't available, log the issue
    if (subset.getPorts().size() > 0) {
      EndpointPort port = subset.getPorts().get(0);
      LOG.debug("Could not find desired protocol ({}), using: ({}:{}:{})", protocol, port.getName(), port.getProtocol(), port.getPort());
      return port.getPort();
    }
    return null;
  }
}

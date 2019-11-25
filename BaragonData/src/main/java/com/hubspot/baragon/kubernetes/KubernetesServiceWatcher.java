package com.hubspot.baragon.kubernetes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.managers.AliasManager;
import com.hubspot.baragon.models.BaragonGroupAlias;
import com.hubspot.baragon.models.BaragonService;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesServiceWatcher extends BaragonKubernetesWatcher<Service> {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceWatcher.class);

  private final KubernetesListener listener;
  private final KubernetesConfiguration kubernetesConfiguration;
  private final AliasManager aliasManager;
  private final ObjectMapper objectMapper;
  private final KubernetesClient kubernetesClient;

  @Inject
  public KubernetesServiceWatcher(KubernetesListener listener,
                                  KubernetesConfiguration kubernetesConfiguration,
                                  AliasManager aliasManager,
                                  ObjectMapper objectMapper,
                                  @Named(KubernetesWatcherModule.BARAGON_KUBERNETES_CLIENT) KubernetesClient kubernetesClient) {
    super();
    this.listener = listener;
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.aliasManager = aliasManager;
    this.objectMapper = objectMapper;
    this.kubernetesClient = kubernetesClient;
  }

  public void createWatch(boolean processInitialFetch) {
    this.processInitialFetch = processInitialFetch;
    ServiceList list = kubernetesClient.services()
        .inAnyNamespace()
        .withLabels(kubernetesConfiguration.getBaragonLabelFilter())
        .withLabel(kubernetesConfiguration.getServiceNameLabel())
        .list();

    if (processInitialFetch) {
      try {
        LOG.info("Got {} initial services from k8s", list.getItems().size());
        list.getItems()
            .forEach(this::processUpdatedService);
      } catch (Throwable t) {
        LOG.error("Exception processing initial endpoints, still attempting to create watcher", t);
      }
    }

    watch = kubernetesClient.services()
        .inAnyNamespace()
        .withLabels(kubernetesConfiguration.getBaragonLabelFilter())
        .withLabel(kubernetesConfiguration.getServiceNameLabel())
        .withResourceVersion(list.getMetadata().getResourceVersion())
        .watch(this);
  }

  @Override
  public void eventReceived(Action action, Service k8sService) {
    LOG.info("Received {} update from k8s for {} ({})", action, k8sService.getMetadata().getName(), getServiceName(k8sService.getMetadata().getLabels()));
    switch (action) {
      case ADDED:
      case MODIFIED:
        processUpdatedService(k8sService);
        break;
      case DELETED:
        processServiceDelete(k8sService);
        break;
      case ERROR:
      default:
        LOG.warn("No handling for action: {} (service: {})", action, k8sService);
    }
  }

  private void processServiceDelete(Service k8sService) {
    Map<String, String> labels = k8sService.getMetadata().getLabels();
    if (labels == null) {
      LOG.warn("No labels present on endpoint {}", k8sService.getMetadata().getName());
      return;
    }
    String serviceName = labels.get(kubernetesConfiguration.getServiceNameLabel());
    if (serviceName == null) {
      LOG.warn("Could not get service name for endpoint update (service: {})", k8sService);
      return;
    }

    String upstreamGroup = labels.getOrDefault(kubernetesConfiguration.getUpstreamGroupsLabel(), "default");
    if (kubernetesConfiguration.getIgnoreUpstreamGroups().contains(upstreamGroup)) {
      LOG.warn("Upstream group not managed by baragon, skipping (service: {})", k8sService);
      return;
    }
    listener.processServiceDelete(serviceName, upstreamGroup);
  }

  private void processUpdatedService(Service k8sService) {
    BaragonService service = buildBaragonService(k8sService);
    if (service == null) {
      LOG.info("Could not build baragon service for {} from {}", getServiceName(k8sService.getMetadata().getLabels()), k8sService);
      return;
    }
    listener.processServiceUpdate(service);
  }

  private Optional<String> getServiceName(Map<String, String> labels) {
    if (labels == null) {
      return Optional.absent();
    }

    return Optional.fromNullable(labels.get(kubernetesConfiguration.getServiceNameLabel()));
  }

  private BaragonService buildBaragonService(Service k8sService) {
    try {
      Map<String, String> labels = k8sService.getMetadata().getLabels();
      if (labels == null) {
        LOG.warn("No labels present on service {}", k8sService.getMetadata().getName());
        return null;
      }
      String serviceName = labels.get(kubernetesConfiguration.getServiceNameLabel());
      if (serviceName == null) {
        LOG.warn("Could not get service name for service update (service: {})", k8sService);
        return null;
      }

      Map<String, String> annotations = k8sService.getMetadata().getAnnotations();
      if (annotations == null) {
        LOG.warn("No annotations present on service {}", k8sService.getMetadata().getName());
        return null;
      }
      String basePath = annotations.get(kubernetesConfiguration.getBasePathAnnotation());
      if (basePath == null) {
        LOG.warn("Could not get base path for service update ( service: {})", k8sService);
        return null;
      }

      String lbGroupsString = annotations.get(kubernetesConfiguration.getLbGroupsAnnotation());
      if (lbGroupsString == null) {
        LOG.warn("Could not get load balancer groups for kubernetes event (service: {})", k8sService);
        return null;
      }
      Optional<String> domainsString = Optional.fromNullable(annotations.get(kubernetesConfiguration.getDomainsAnnotation()));
      // non-aliased edgeCacheDomains not yet supported
      BaragonGroupAlias groupsAndDomains = aliasManager.processAliases(
          new HashSet<>(Arrays.asList(lbGroupsString.split(","))),
          domainsString.transform((d) -> new HashSet<>(Arrays.asList(d.split(",")))).or(new HashSet<>()),
          Collections.emptySet()
      );

      Optional<String> ownerString = Optional.fromNullable(annotations.get(kubernetesConfiguration.getOwnersAnnotation()));
      List<String> owners = ownerString.transform((o) -> Arrays.asList(o.split(","))).or(new ArrayList<>());

      Optional<String> templateName = Optional.fromNullable(annotations.get(kubernetesConfiguration.getTemplateNameAnnotation()));

      Map<String, Object> customConfig = annotations.containsKey(kubernetesConfiguration.getCustomConfigAnnotation()) ?
          objectMapper.readValue(annotations.get(kubernetesConfiguration.getCustomConfigAnnotation()), new TypeReference<Map<String, Object>>() {}) :
          Collections.emptyMap();

      String additionalPathsString = annotations.get(kubernetesConfiguration.getAdditionalPathsAnnotation());
      List<String> additionalPaths = additionalPathsString != null ?
          Arrays.asList(additionalPathsString.split(",")) :
          Collections.emptyList();

      return new BaragonService(
          serviceName,
          owners,
          basePath,
          additionalPaths,
          groupsAndDomains.getGroups(),
          customConfig,
          templateName,
          groupsAndDomains.getDomains(),
          Optional.absent(),
          groupsAndDomains.getEdgeCacheDomains(),
          false // Always using IPs
      );
    } catch (Throwable t) {
      LOG.error("Unable to build BaragonService object from endpoint {}", k8sService.getMetadata().getName(), t);
      return null;
    }
  }
}

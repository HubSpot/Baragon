package com.hubspot.baragon.kubernetes.listeners;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.KubernetesIntegrationConfiguration;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.OwnerReference;

public class EndpointsAddedOrModifiedListener implements KubernetesEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(EndpointsAddedOrModifiedListener.class);

  private final BaragonServiceClient baragonClient;
  private final KubernetesIntegrationConfiguration kubesConfig;

  public EndpointsAddedOrModifiedListener(BaragonServiceClient baragonClient,
                                 BaragonConfiguration baragonConfiguration) {
    this.baragonClient = baragonClient;
    this.kubesConfig = baragonConfiguration.getKubernetesIntegrationConfiguration().get();
  }

  public void handleEvent(Endpoints endpoints) {
    endpoints.getMetadata().getAdditionalProperties();
    Map<String, String> annotations = endpoints.getMetadata().getAnnotations();


    String appRoot = annotations.get(kubesConfig.getAppRootKey());
    String deployableName = annotations.get(kubesConfig.getServiceNameKey());
    // Or endpoints.getMetadata().getName() ?
    Optional<String> lbTemplate = Optional.fromNullable(annotations.get(kubesConfig.getLoadBalancerTemplateKey()));
    Set<String> lbGroups = new HashSet<>(Arrays.asList(annotations.get(kubesConfig.getLbGroupsKey()).replaceAll("\\s", "").split(",")));

    // Assuming Orion is passing the relevant information in the metadata annotations, we don't have any need
    // to go through the each EndpointSubset to figure out what needs to be added/removed.

    // Or get the pods and then get the labels on those? some of this info already there, but is an extra API call
    final BaragonService lbService = new BaragonService(
        deployableName,
        endpoints.getMetadata().getOwnerReferences().stream().map(OwnerReference::getName).collect(Collectors.toList()),
        // ^ Empty for whoami
        appRoot,
        Collections.emptyList(),
        lbGroups,
        null,
        lbTemplate,
        Collections.emptySet());


    String hostname = "";
    String port = "";
    String rackId = "";
    String lbRequestId = "";

    // Or get from pod Node field? No port included there tho, plus extra API call
    String upstream = String.format("%s:%d", hostname, port);
    List<UpstreamInfo> requestedUpstreams = lbGroups.stream()
        .map(group ->
            new UpstreamInfo(upstream, Optional.of(deployableName), Optional.of(rackId), Optional.of(group)))
        .collect(Collectors.toList());

    for (String group : lbGroups) {
      Optional<BaragonService> service = baragonClient.getServiceForBasePath(group, appRoot);
      if (service.isPresent()) {
        baragonClient.setUpstreams(service.get().getServiceId(), requestedUpstreams);
      } else {
        BaragonRequest request = new BaragonRequest(lbRequestId, lbService, requestedUpstreams, Collections.emptyList());
        baragonClient.enqueueRequest(request);
      }
    }
  }
}

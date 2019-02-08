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
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.OwnerReference;

public class DeploymentAddedListener implements KubernetesEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(DeploymentAddedListener.class);

  // TODO: Verify that these are the field names Orion is placing in K8s metadata
  private static final String APP_ROOT = "appRoot";
  private static final String SERVICE_NAME = "serviceName";
  private static final String LOAD_BALANCER_TEMPLATE = "loadBalancerTemplate";
  private static final String LB_GROUPS = "loadBalancerGroups";


  public void handleEvent(Event event) {
    event.getMetadata().getAdditionalProperties();
    Map<String, String> annotations = event.getMetadata().getAnnotations();

    String hostname = "";
    String port = "";
    String rackId = "";
    String lbRequestId = "";

    String appRoot = annotations.get(APP_ROOT);
    String deployableName = annotations.get(SERVICE_NAME);
    Optional<String> lbTemplate = Optional.fromNullable(annotations.get(LOAD_BALANCER_TEMPLATE));
    Set<String> lbGroups = new HashSet<>(Arrays.asList(annotations.get(LB_GROUPS).replaceAll("\\s", "").split(",")));

    final BaragonService lbService = new BaragonService(
        deployableName,
        event.getMetadata().getOwnerReferences().stream().map(OwnerReference::getName).collect(Collectors.toList()),
        appRoot,
        Collections.emptyList(),
        lbGroups,
        null,
        lbTemplate,
        Collections.emptySet());


    String upstream = String.format("%s:%d", hostname, port);
    List<UpstreamInfo> upstreams = lbGroups.stream()
        .map(group ->
            new UpstreamInfo(upstream, Optional.of(deployableName), Optional.of(rackId), Optional.of(group)))
        .collect(Collectors.toList());

    new BaragonRequest(lbRequestId, lbService, upstreams, Collections.emptyList());

  }
}

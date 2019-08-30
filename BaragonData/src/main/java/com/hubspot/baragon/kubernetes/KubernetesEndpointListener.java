package com.hubspot.baragon.kubernetes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public abstract class KubernetesEndpointListener {
  private final BaragonStateDatastore stateDatastore;
  private final KubernetesConfiguration kubernetesConfiguration;

  public KubernetesEndpointListener(BaragonStateDatastore stateDatastore,
                                    KubernetesConfiguration kubernetesConfiguration) {
    this.stateDatastore = stateDatastore;
    this.kubernetesConfiguration = kubernetesConfiguration;
  }

  public abstract void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams);

  protected BaragonRequest createBaragonRequest(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    List<UpstreamInfo> nonK8sUpstreams = stateDatastore.getUpstreams(updatedService.getServiceId())
        .stream()
        .filter((u) -> !kubernetesConfiguration.getUpstreamGroups().contains(u.getGroup()))
        .collect(Collectors.toList());
    List<UpstreamInfo> allUpstreams = new ArrayList<>();
    allUpstreams.addAll(activeUpstreams);
    allUpstreams.addAll(nonK8sUpstreams);

    BaragonService relevantService;
    if (nonK8sUpstreams.isEmpty()) {
      relevantService = updatedService;
    } else {
      // K8s integration supports a subset of features, take existing extra options if non-k8s is also present
      relevantService = stateDatastore.getService(updatedService.getServiceId()).or(updatedService);
    }

    String requestId = String.format("k8s-%d", System.currentTimeMillis());

    return new BaragonRequest(
        requestId,
        relevantService,
        Collections.emptyList(),
        Collections.emptyList(),
        allUpstreams,
        Optional.absent(),
        Optional.of(RequestAction.UPDATE),
        false,
        false,
        false,
        false
    );
  }
}

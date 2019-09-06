package com.hubspot.baragon.kubernetes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public abstract class KubernetesEndpointListener {
  protected final BaragonStateDatastore stateDatastore;
  protected final KubernetesConfiguration kubernetesConfiguration;

  public KubernetesEndpointListener(BaragonStateDatastore stateDatastore,
                                    KubernetesConfiguration kubernetesConfiguration) {
    this.stateDatastore = stateDatastore;
    this.kubernetesConfiguration = kubernetesConfiguration;
  }

  public abstract void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams);

  protected BaragonRequest createBaragonRequest(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    return createBaragonRequest(updatedService, activeUpstreams, stateDatastore.getUpstreams(updatedService.getServiceId()));
  }

  protected BaragonRequest createBaragonRequest(BaragonService updatedService, List<UpstreamInfo> activeUpstreams, Collection<UpstreamInfo> existingUpstreams) {
    Map<Boolean, List<UpstreamInfo>> partitionedUpstreams = existingUpstreams.stream()
        .collect(Collectors.partitioningBy((u) -> kubernetesConfiguration.getUpstreamGroups().contains(u.getGroup())));

    List<UpstreamInfo> nonK8sUpstreams = partitionedUpstreams.getOrDefault(false, Collections.emptyList());
    List<UpstreamInfo> toRemove = partitionedUpstreams.getOrDefault(true, Collections.emptyList())
        .stream()
        .filter((u) -> !activeUpstreams.contains(u))
        .collect(Collectors.toList());

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
        activeUpstreams,
        toRemove,
        Collections.emptyList(),
        Optional.absent(),
        Optional.of(RequestAction.UPDATE),
        false,
        false,
        false,
        false
    );
  }
}

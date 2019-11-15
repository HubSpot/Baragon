package com.hubspot.baragon.kubernetes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public abstract class KubernetesListener {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesListener.class);

  protected final BaragonStateDatastore stateDatastore;
  protected final KubernetesConfiguration kubernetesConfiguration;

  public KubernetesListener(BaragonStateDatastore stateDatastore,
                            KubernetesConfiguration kubernetesConfiguration) {
    this.stateDatastore = stateDatastore;
    this.kubernetesConfiguration = kubernetesConfiguration;
  }

  public abstract void processUpstreamsUpdate(String serviceName, String upstreamGroup, List<UpstreamInfo> activeUpstreams);

  public abstract void processServiceUpdate(BaragonService updatedService);

  public abstract void processEndpointsDelete(String serviceName, String upstreamGroup);

  public abstract void processServiceDelete(String serviceName, String upstreamGroup);

  protected BaragonRequest createDeleteRequest(BaragonService service) {
    String requestId = String.format("k8s-delete-%d", System.nanoTime());
    return new BaragonRequestBuilder()
        .setAction(Optional.of(RequestAction.DELETE))
        .setLoadBalancerRequestId(requestId)
        .setLoadBalancerService(service)
        .build();
  }
}

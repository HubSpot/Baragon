package com.hubspot.baragon.service.kubernetes;

import java.util.List;

import com.google.inject.Inject;
import com.hubspot.baragon.kubernetes.KubernetesEndpointListener;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonServiceKubernetesListener implements KubernetesEndpointListener {

  @Inject
  public BaragonServiceKubernetesListener() {

  }

  public void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    // TODO - lock on state update? might conflict with request manager updates
    // merge with existing upstreams + apply to global state
  }
}

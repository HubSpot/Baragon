package com.hubspot.baragon.agent.kubernetes;

import java.util.List;

import com.google.inject.Inject;
import com.hubspot.baragon.kubernetes.KubernetesEndpointListener;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonAgentKubernetesListener implements KubernetesEndpointListener {

  @Inject
  public BaragonAgentKubernetesListener() {

  }

  public void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    // filter for relevant group + merge with existing upstreams + apply updates
  }
}

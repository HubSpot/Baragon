package com.hubspot.baragon.kubernetes;

import java.util.List;

import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;

public interface KubernetesEndpointListener {
  void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams);
}

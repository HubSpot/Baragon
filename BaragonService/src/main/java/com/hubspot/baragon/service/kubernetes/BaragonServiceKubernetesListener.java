package com.hubspot.baragon.service.kubernetes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.kubernetes.KubernetesEndpointListener;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.managers.RequestManager;

public class BaragonServiceKubernetesListener extends KubernetesEndpointListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonServiceKubernetesListener.class);

  private final RequestManager requestManager;

  @Inject
  public BaragonServiceKubernetesListener(BaragonStateDatastore stateDatastore,
                                          KubernetesConfiguration kubernetesConfiguration,
                                          RequestManager requestManager) {
    super(stateDatastore, kubernetesConfiguration);
    this.requestManager = requestManager;
  }

  @Override
  public void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    try {
      BaragonRequest baragonRequest = createBaragonRequest(updatedService, activeUpstreams);
      requestManager.commitRequest(baragonRequest);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    }
  }
}

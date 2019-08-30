package com.hubspot.baragon.agent.kubernetes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.kubernetes.KubernetesEndpointListener;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonAgentKubernetesListener implements KubernetesEndpointListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonAgentKubernetesListener.class);

  private final KubernetesConfiguration kubernetesConfiguration;
  private final BaragonAgentConfiguration agentConfiguration;
  private final AgentRequestManager agentRequestManager;
  private final BaragonStateDatastore stateDatastore;

  @Inject
  public BaragonAgentKubernetesListener(KubernetesConfiguration kubernetesConfiguration,
                                        BaragonAgentConfiguration agentConfiguration,
                                        AgentRequestManager agentRequestManager,
                                        BaragonStateDatastore stateDatastore) {
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.agentConfiguration = agentConfiguration;
    this.agentRequestManager = agentRequestManager;
    this.stateDatastore = stateDatastore;
  }

  public void processUpdate(BaragonService updatedService, List<UpstreamInfo> activeUpstreams) {
    // filter for relevant group + merge with existing upstreams + apply updates
    if (!isRelevantUpdate(updatedService)) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", updatedService.getServiceId());
      LOG.trace("Skipped update {} - {}", updatedService, activeUpstreams);
      return;
    }

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

    BaragonRequest baragonRequest = new BaragonRequest(
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
    agentRequestManager.processRequest(requestId, RequestAction.UPDATE, baragonRequest, Optional.absent(), Collections.emptyMap(), false, Optional.absent());
  }

  private boolean isRelevantUpdate(BaragonService updatedService) {
    boolean isUpdateForGroup = updatedService.getLoadBalancerGroups().contains(agentConfiguration.getLoadBalancerConfiguration().getName());
    boolean isUpdateForDomain = !Sets.intersection(updatedService.getDomains(), agentConfiguration.getLoadBalancerConfiguration().getDomains()).isEmpty();
    boolean isUpdateForDefaultDomain = updatedService.getDomains().contains(agentConfiguration.getLoadBalancerConfiguration().getDefaultDomain().or(""));
    return isUpdateForGroup && (isUpdateForDomain || isUpdateForDefaultDomain);
  }
}

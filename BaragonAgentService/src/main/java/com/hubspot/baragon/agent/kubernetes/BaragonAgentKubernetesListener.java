package com.hubspot.baragon.agent.kubernetes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.kubernetes.KubernetesListener;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonAgentKubernetesListener extends KubernetesListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonAgentKubernetesListener.class);

  private final BaragonAgentConfiguration agentConfiguration;
  private final AgentRequestManager agentRequestManager;

  @Inject
  public BaragonAgentKubernetesListener(KubernetesConfiguration kubernetesConfiguration,
                                        BaragonAgentConfiguration agentConfiguration,
                                        AgentRequestManager agentRequestManager,
                                        BaragonStateDatastore stateDatastore) {
    super(stateDatastore, kubernetesConfiguration);
    this.agentConfiguration = agentConfiguration;
    this.agentRequestManager = agentRequestManager;
  }

  @Override
  public void processServiceDelete(String serviceId, String upstreamGroup) {
    // TODO - if upstreams remain in other groups, only delete the relevant upstreams
    BaragonRequest baragonRequest = createDeleteRequest(service);
    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.DELETE,
        baragonRequest,
        Optional.absent(),
        Collections.singletonMap(serviceId, Collections.emptyList()),
        false,
        Optional.absent());
  }

  @Override
  public void processServiceUpdate(BaragonService updatedService) {
    if (!isRelevantUpdate(updatedService)) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", updatedService.getServiceId());
      LOG.trace("Skipped update {} - {}", updatedService, activeUpstreams);
      return;
    }

    Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(updatedService.getServiceId());

    BaragonRequest baragonRequest = createBaragonRequest(updatedService, existingUpstreams, existingUpstreams);

    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.UPDATE,
        baragonRequest,
        Optional.absent(),
        Collections.singletonMap(updatedService.getServiceId(), existingUpstreams),
        false,
        Optional.absent());
  }

  @Override
  public void processUpstreamsUpdate(String serviceName, String upstreamGroup, List<UpstreamInfo> activeupstreams) {
    // TODO - only replace upstreams of the specified group
    if (!isRelevantUpdate(updatedService)) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", updatedService.getServiceId());
      LOG.trace("Skipped update {} - {}", updatedService, activeUpstreams);
      return;
    }

    Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(updatedService.getServiceId());

    BaragonRequest baragonRequest = createBaragonRequest(updatedService, existingUpstreams, existingUpstreams);

    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.UPDATE,
        baragonRequest,
        Optional.absent(),
        Collections.singletonMap(updatedService.getServiceId(), existingUpstreams),
        false,
        Optional.absent());
  }

  @Override
  public void processEndpointsDelete(String serviceName, String upstreamGroup) {
    // TODO - if upstreams remain in other groups, only delete the relevant upstreams
    // if service exists, leave as a service with no upstreams
    if (!isRelevantUpdate(updatedService)) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", updatedService.getServiceId());
      LOG.trace("Skipped update {} - {}", updatedService, activeUpstreams);
      return;
    }

    Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(updatedService.getServiceId());

    BaragonRequest baragonRequest = createBaragonRequest(updatedService, existingUpstreams, existingUpstreams);

    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.UPDATE,
        baragonRequest,
        Optional.absent(),
        Collections.singletonMap(updatedService.getServiceId(), existingUpstreams),
        false,
        Optional.absent());
  }

  private boolean isRelevantUpdate(BaragonService updatedService) {
    boolean isUpdateForGroup = updatedService.getLoadBalancerGroups().contains(agentConfiguration.getLoadBalancerConfiguration().getName());
    boolean isUpdateForDomain = !Sets.intersection(updatedService.getDomains(), agentConfiguration.getLoadBalancerConfiguration().getDomains()).isEmpty();
    boolean isUpdateForDefaultDomain = updatedService.getDomains().contains(agentConfiguration.getLoadBalancerConfiguration().getDefaultDomain().or(""));
    return isUpdateForGroup && (isUpdateForDomain || isUpdateForDefaultDomain);
  }
}

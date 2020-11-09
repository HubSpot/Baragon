package com.hubspot.baragon.agent.kubernetes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  // TODO - lock on all these? On ServiceId?

  @Override
  public void processServiceDelete(String serviceId, String upstreamGroup) {
    processDelete(serviceId, upstreamGroup);
  }

  @Override
  public void processServiceUpdate(BaragonService updatedService) {
    if (!isRelevantUpdate(updatedService)) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", updatedService.getServiceId());
      LOG.trace("Skipped update {}", updatedService);
      return;
    }

    List<UpstreamInfo> existingUpstreams = new ArrayList<>(stateDatastore.getUpstreams(updatedService.getServiceId()));
    Map<Boolean, List<UpstreamInfo>> partitionedUpstreams = existingUpstreams.stream()
        .collect(Collectors.partitioningBy((u) -> kubernetesConfiguration.getIgnoreUpstreamGroups().contains(u.getGroup())));

    // K8s integration supports a subset of features, take existing extra options if non-k8s upstreams also present
    BaragonService relevantService = partitionedUpstreams.get(true).isEmpty() ?
        updatedService :
        stateDatastore.getService(updatedService.getServiceId()).or(updatedService);
    BaragonRequest baragonRequest = new BaragonRequest(
        String.format("k8s-update-service-%d", System.nanoTime()),
        relevantService,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Optional.absent(),
        Optional.of(RequestAction.UPDATE),
        false,
        false,
        false,
        false
    );

    Map<String, Collection<UpstreamInfo>> upstreamsForUpdate = new HashMap<>();
    upstreamsForUpdate.put(updatedService.getServiceId(), existingUpstreams);

    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.UPDATE,
        baragonRequest,
        Optional.absent(),
        upstreamsForUpdate,
        false,
        Optional.absent());
  }

  @Override
  public void processUpstreamsUpdate(String serviceId, String upstreamGroup, List<UpstreamInfo> activeUpstreams) {
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (!maybeService.isPresent()) {
      LOG.info("No service definition for {}, skipping update", serviceId);
      LOG.trace("Skipped update {} - {}", serviceId, activeUpstreams);
      return;
    }
    if (!isRelevantUpdate(maybeService.get())) {
      LOG.debug("Not relevant update for agent's group/domains, skipping ({})", serviceId);
      LOG.trace("Skipped update {} - {}", serviceId, activeUpstreams);
      return;
    }

    Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(serviceId);

    List<UpstreamInfo> toRemove = existingUpstreams
        .stream()
        .filter((u) -> {
          boolean groupMatches = u.getGroup().equals(upstreamGroup);
          if (!groupMatches) {
            return false;
          }
          for (UpstreamInfo active : activeUpstreams) {
            if (UpstreamInfo.upstreamAndGroupMatches(u, active)) {
              return false;
            }
          }
          return true;
        })
        .collect(Collectors.toList());

    BaragonRequest baragonRequest = new BaragonRequest(
        String.format("k8s-update-uptreams-%d", System.nanoTime()),
        maybeService.get(),
        activeUpstreams,
        toRemove,
        Collections.emptyList(),
        Optional.absent(),
        Optional.of(RequestAction.UPDATE),
        false,
        false,
        true,
        false
    );

    Map<String, Collection<UpstreamInfo>> upstreamsForUpdate = new HashMap<>();
    upstreamsForUpdate.put(serviceId, existingUpstreams);

    agentRequestManager.processRequest(
        baragonRequest.getLoadBalancerRequestId(),
        RequestAction.UPDATE,
        baragonRequest,
        Optional.absent(),
        upstreamsForUpdate,
        false,
        Optional.absent());
  }

  @Override
  public void processEndpointsDelete(String serviceName, String upstreamGroup) {
    processDelete(serviceName, upstreamGroup);
  }

  private boolean isRelevantUpdate(BaragonService updatedService) {
    boolean isUpdateForGroup = updatedService.getLoadBalancerGroups().contains(agentConfiguration.getLoadBalancerConfiguration().getName());
    boolean isUpdateForDomain = !Sets.intersection(updatedService.getDomains(), agentConfiguration.getLoadBalancerConfiguration().getDomains()).isEmpty();
    boolean isUpdateForDefaultDomain = updatedService.getDomains().contains(agentConfiguration.getLoadBalancerConfiguration().getDefaultDomain().or(""));
    return isUpdateForGroup && (isUpdateForDomain || isUpdateForDefaultDomain);
  }

  private void processDelete(String serviceId, String upstreamGroup) {
    Map<Boolean, List<UpstreamInfo>> partitionedUpstreams = stateDatastore.getUpstreams(serviceId)
        .stream()
        .collect(Collectors.partitioningBy((u) -> u.getGroup().equals(upstreamGroup)));
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (partitionedUpstreams.get(false).isEmpty()) {
      LOG.info("No remaining upstreams for {}, deleting", serviceId);
      if (maybeService.isPresent()) {
        BaragonRequest baragonRequest = createDeleteRequest(maybeService.get());
        agentRequestManager.processRequest(
            baragonRequest.getLoadBalancerRequestId(),
            RequestAction.DELETE,
            baragonRequest,
            Optional.absent(),
            new HashMap<>(),
            false,
            Optional.absent());
      } else {
        LOG.warn("No service present for {} to process endpoints delete", serviceId);
      }
    } else if (maybeService.isPresent()) {
      LOG.info("Received service delete, but upstreams in other groups remain, removing upstreams for group {} from {}", upstreamGroup, serviceId);
      BaragonRequest request = new BaragonRequest(
          String.format("k8s-delete-%d", System.nanoTime()),
          maybeService.get(),
          Collections.emptyList(),
          partitionedUpstreams.get(true),
          Collections.emptyList(),
          Optional.absent(),
          Optional.of(RequestAction.UPDATE),
          false,
          false,
          false,
          false
      );

      Map<String, Collection<UpstreamInfo>> upstreamsForUpdate = new HashMap<>();
      upstreamsForUpdate.put(serviceId, partitionedUpstreams.get(false));

      agentRequestManager.processRequest(
          request.getLoadBalancerRequestId(),
          RequestAction.UPDATE,
          request,
          Optional.absent(),
          upstreamsForUpdate,
          false,
          Optional.absent());
    } else {
      LOG.warn("No service present for {} to process endpoints delete", serviceId);
    }
  }
}

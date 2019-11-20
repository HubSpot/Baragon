package com.hubspot.baragon.service.kubernetes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.kubernetes.KubernetesListener;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.managers.RequestManager;

public class BaragonServiceKubernetesListener extends KubernetesListener {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonServiceKubernetesListener.class);

  private final RequestManager requestManager;
  private final ReentrantLock lock;

  @Inject
  public BaragonServiceKubernetesListener(BaragonStateDatastore stateDatastore,
                                          KubernetesConfiguration kubernetesConfiguration,
                                          RequestManager requestManager,
                                          @Named(BaragonServiceModule.REQUEST_LOCK) ReentrantLock lock) {
    super(stateDatastore, kubernetesConfiguration);
    this.requestManager = requestManager;
    this.lock = lock;
  }

  @Override
  public void processServiceDelete(String serviceId, String upstreamGroup) {
    lock.lock();
    try {
      processDelete(serviceId, upstreamGroup);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void processServiceUpdate(BaragonService updatedService) {
    lock.lock();
    try {
      LOG.info("acquired lock for service update on {}", updatedService.getServiceId());
      Optional<BaragonService> existingService = stateDatastore.getService(updatedService.getServiceId());
      List<UpstreamInfo> existingUpstreams = new ArrayList<>(stateDatastore.getUpstreams(updatedService.getServiceId()));
      Map<Boolean, List<UpstreamInfo>> partitionedUpstreams = existingUpstreams.stream()
          .collect(Collectors.partitioningBy((u) -> kubernetesConfiguration.getIgnoreUpstreamGroups().contains(u.getGroup())));

      // K8s integration supports a subset of features, take existing extra options if non-k8s upstreams also present
      if (!existingService.isPresent() || (!existingService.get().equals(updatedService) && partitionedUpstreams.get(true).isEmpty())) {
        BaragonRequest baragonRequest = new BaragonRequest(
            String.format("k8s-update-service-%d", System.nanoTime()),
            updatedService,
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
        requestManager.commitRequest(baragonRequest);
      } else {
        LOG.info("No update to service {} (existing {}), skipping", updatedService, existingService);
      }
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void processUpstreamsUpdate(String serviceId, String upstreamGroup, List<UpstreamInfo> activeUpstreams) {
    lock.lock();
    try {
      LOG.info("acquired lock for upstreams update on {}", serviceId);
      Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
      if (!maybeService.isPresent()) {
        LOG.info("No service definition for {}, skipping update", serviceId);
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

      requestManager.commitRequest(baragonRequest);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void processEndpointsDelete(String serviceId, String upstreamGroup) {
    lock.lock();
    try {
      processDelete(serviceId, upstreamGroup);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  private void processDelete(String serviceId, String upstreamGroup) throws Exception {
    Map<Boolean, List<UpstreamInfo>> partitionedUpstreams = stateDatastore.getUpstreams(serviceId)
        .stream()
        .collect(Collectors.partitioningBy((u) -> u.getGroup().equals(upstreamGroup)));
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (partitionedUpstreams.get(false).isEmpty()) {
      LOG.info("No remaining upstreams for {}, deleting", serviceId);
      if (maybeService.isPresent()) {
        BaragonRequest baragonRequest = createDeleteRequest(maybeService.get());
        requestManager.commitRequest(baragonRequest);
      } else {
        LOG.warn("No service present for {} to process delete", serviceId);
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
      requestManager.commitRequest(request);
    } else {
      LOG.warn("No service present for {} to process endpoints delete", serviceId);
    }
  }
}

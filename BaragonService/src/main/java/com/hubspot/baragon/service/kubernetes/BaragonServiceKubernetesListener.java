package com.hubspot.baragon.service.kubernetes;

import java.util.HashMap;
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
  public void processServiceDelete(String serviceName, String upstreamGroup) {
    // TODO - if upstreams remain in other groups, only delete the relevant upstreams
    lock.lock();
    try {
      BaragonRequest baragonRequest = createDeleteRequest(service);
      requestManager.commitRequest(baragonRequest);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void processServiceUpdate(BaragonService updatedService) {
    // TODO
    lock.lock();
    try {
      Optional<BaragonService> existing = stateDatastore.getService(updatedService.getServiceId());
      if (existing.isPresent() && !shouldUpdate(updatedService, activeUpstreams, existing.get())) {
        LOG.debug("No changes in service {}, skipping update from k8s watcher", updatedService.getServiceId());
      }

      BaragonRequest baragonRequest = createBaragonRequest(updatedService, activeUpstreams);
      requestManager.commitRequest(baragonRequest);
    } catch (Throwable t) {
      LOG.error("Could not commit update from kubernetes watcher", t);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void processUpstreamsUpdate(String serviceName, String upstreamGroup, List<UpstreamInfo> activeUpstreams) {
    // only update the specified group
  }

  @Override
  public void processEndpointsDelete(String serviceName, String upstreamGroup) {
    // TODO - if upstreams remain in other groups, only delete the relevant upstreams
    // if service exists, leave as a service with no upstreams
  }

  private boolean shouldUpdate(BaragonService updatedService, List<UpstreamInfo> newUpstreams, BaragonService existing) {
    List<UpstreamInfo> existingK8sUpstreams = stateDatastore.getUpstreams(updatedService.getServiceId()).stream()
        .filter((u) -> kubernetesConfiguration.getUpstreamGroups().contains(u.getGroup()))
        .collect(Collectors.toList());
    return !updatedService.equals(existing) || !haveSameElements(newUpstreams, existingK8sUpstreams);
  }


  private <T> boolean haveSameElements(final List<T> list1, final List<T> list2) {
    if (list1 == list2) {
      return true;
    }

    if (list1 == null || list2 == null || list1.size() != list2.size()) {
      return false;
    }

    Map<T, Count> counts = new HashMap<>();

    for (T item : list1) {
      if (!counts.containsKey(item)){
        counts.put(item, new Count());
      }
      counts.get(item).count += 1;
    }

    for (T item : list2) {
      if (!counts.containsKey(item)) {
        return false;
      }
      counts.get(item).count -= 1;
    }

    for (Map.Entry<T, Count> entry : counts.entrySet()) {
      if (entry.getValue().count != 0) {
        return false;
      }
    }

    return true;
  }

  private static class Count {
    int count = 0;
  }
}

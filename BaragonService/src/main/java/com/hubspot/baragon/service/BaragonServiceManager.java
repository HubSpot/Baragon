package com.hubspot.baragon.service;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.exceptions.PendingServiceOccupiedException;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.webhooks.WebhookEvent;
import com.hubspot.baragon.webhooks.WebhookNotifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

public class BaragonServiceManager {
  private static final Log LOG = LogFactory.getLog(BaragonServiceManager.class);

  private final BaragonDataStore datastore;
  private final LoadBalancerManager loadBalancerManager;
  private final WebhookNotifier webhookNotifier;

  @Inject
  public BaragonServiceManager(BaragonDataStore datastore, LoadBalancerManager loadBalancerManager,
                               WebhookNotifier webhookNotifier) {
    this.datastore = datastore;
    this.loadBalancerManager = loadBalancerManager;
    this.webhookNotifier = webhookNotifier;
  }

  public void addPendingService(ServiceInfo serviceInfo) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(serviceInfo.getName());

    if (maybeServiceInfo.isPresent()) {
      throw new PendingServiceOccupiedException(maybeServiceInfo.get());
    }

    LOG.info(String.format("Adding service %s %s by %s", serviceInfo.getName(), serviceInfo.getId(), serviceInfo.getContactEmail()));

    datastore.addPendingService(serviceInfo);
    webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.SERVICE_ADDED, null, serviceInfo, null));
  }

  public boolean removePendingService(String serviceName) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(serviceName);

    if (maybeServiceInfo.isPresent()) {
      datastore.removePendingService(serviceName);
      webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.SERVICE_ADDED, null, maybeServiceInfo.get(), null));
      return true;
    }

    return false;
  }

  public void activateService(String name) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException(String.format("No such pending service: %s", name));
    }

    Collection<String> upstreams = datastore.getHealthyUpstreams(name, maybeServiceInfo.get().getId());

    for (String lb : maybeServiceInfo.get().getLbs()) {
      LOG.info("   Applying to " + lb);
      loadBalancerManager.apply(lb, maybeServiceInfo.get(), upstreams);
    }

    datastore.makeServiceActive(name);
    webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.SERVICE_ACTIVE, null, maybeServiceInfo.get(), null));
  }

  public Optional<ServiceInfo> getActiveService(String name) {
    return datastore.getActiveService(name);
  }

  public Optional<ServiceInfo> getPendingService(String name) {
    return datastore.getPendingService(name);
  }

  public void syncUpstreams(String name) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (maybeServiceInfo.isPresent()) {
      Collection<String> upstreams = datastore.getHealthyUpstreams(maybeServiceInfo.get().getName(), maybeServiceInfo.get().getId());

      for (String lb : maybeServiceInfo.get().getLbs()) {
        LOG.info("   Applying to " + lb);
        loadBalancerManager.apply(lb, maybeServiceInfo.get(), upstreams);
      }
    }
  }

  public void addUnhealthyUpstream(String name, String id, String upstream) {
    datastore.addUnhealthyUpstream(name, id, upstream);
  }

  public void addActiveUpstream(String name, String upstream) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("No such service");
    }

    datastore.addUnhealthyUpstream(maybeServiceInfo.get().getName(), maybeServiceInfo.get().getId(), upstream);
  }

  public void makeUpstreamHealthy(String name, String id, String upstream) {
    datastore.makeUpstreamHealthy(name, id, upstream);
  }

  public void removeUpstream(String name, String id, String upstream) {
    datastore.removeHealthyUpstream(name, id, upstream);
    datastore.removeUnhealthyUpstream(name, id, upstream);
  }

  public Collection<String> getHealthyUpstreams(String name, String id) {
    return datastore.getHealthyUpstreams(name, id);
  }

  public Collection<String> getUnhealthyUpstreams(String name, String id) {
    return datastore.getUnhealthyUpstreams(name, id);
  }
}

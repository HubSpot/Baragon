package com.hubspot.baragon.service;

import java.util.Collection;

import com.hubspot.baragon.data.BaragonDataStore;

import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LoadBalancerManager;

public class BaragonServiceManager {
  private static final Log LOG = LogFactory.getLog(BaragonServiceManager.class);

  private final BaragonDataStore datastore;
  private final LoadBalancerManager loadBalancerManager;

  @Inject
  public BaragonServiceManager(BaragonDataStore datastore, LoadBalancerManager loadBalancerManager) {
    this.datastore = datastore;
    this.loadBalancerManager = loadBalancerManager;
  }

  public void addPendingService(ServiceInfo serviceInfo) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(serviceInfo.getName());

    if (maybeServiceInfo.isPresent()) {
      throw new RuntimeException(String.format("Already a pending service by %s", maybeServiceInfo.get().getContactEmail()));
    }

    LOG.info(String.format("Adding service %s %s by %s", serviceInfo.getName(), serviceInfo.getId(), serviceInfo.getContactEmail()));

    datastore.addPendingService(serviceInfo);
  }

  public boolean removePendingService(String serviceName) {
    return datastore.removePendingService(serviceName);
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

  public void addPendingUpstream(String name, String upstream) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("No such service");
    }

    datastore.addUnhealthyUpstream(maybeServiceInfo.get().getName(), maybeServiceInfo.get().getId(), upstream);
  }

  public void addActiveUpstream(String name, String upstream) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("No such service");
    }

    datastore.addUnhealthyUpstream(maybeServiceInfo.get().getName(), maybeServiceInfo.get().getId(), upstream);
  }

  public void markUpstreamHealthy(String name, String upstream) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("No such service");
    }

    datastore.makeUpstreamHealthy(maybeServiceInfo.get().getName(), maybeServiceInfo.get().getId(), upstream);
  }

  public void removeUpstream(String name, String id, String upstream) {
    // TODO: implement
  }

  public Collection<String> getHealthyUpstreams(String name) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("Service is not active");
    }

    return datastore.getHealthyUpstreams(name, maybeServiceInfo.get().getId());
  }

  public Collection<String> getUnhealthyUpstreams(String name) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException("Service is not active");
    }

    return datastore.getUnhealthyUpstreams(name, maybeServiceInfo.get().getId());
  }
}

package com.hubspot.baragon.managers;

import java.util.Collection;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;

public class ServiceManager {
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public ServiceManager(BaragonStateDatastore stateDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.stateDatastore = stateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  public Collection<BaragonServiceState> getAllServices() {
    return stateDatastore.getGlobalState();
  }

  public Optional<BaragonServiceState> getService(String serviceId) {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    try {
      return Optional.of(new BaragonServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreamsMap(serviceId).values()));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<BaragonServiceState> removeService(String serviceId) {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    stateDatastore.removeService(serviceId);
    for (String loadBalancerGroup : maybeServiceInfo.get().getLoadBalancerGroups()) {
      loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeServiceInfo.get().getServiceBasePath());
    }

    stateDatastore.updateStateNode();

    try {
      return Optional.of(new BaragonServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreamsMap(serviceId).values()));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

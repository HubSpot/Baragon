package com.hubspot.baragon.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.models.ServiceSnapshot;

import java.util.Collection;
import java.util.Collections;

@Singleton
public class SnapshotUtils {
  private final BaragonDataStore datastore;

  @Inject
  public SnapshotUtils(BaragonDataStore datastore) {
    this.datastore = datastore;
  }

  public ServiceSnapshot buildSnapshot(ServiceInfo serviceInfo) {
    final Collection<String> healthyUpstreams = datastore.getHealthyUpstreams(serviceInfo.getName(), serviceInfo.getId());
    final Collection<String> unhealthyUpstreams = datastore.getUnhealthyUpstreams(serviceInfo.getName(), serviceInfo.getId());

    return new ServiceSnapshot(serviceInfo, healthyUpstreams, unhealthyUpstreams, Collections.<String>emptyList(), System.currentTimeMillis());
  }
}

package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.UpstreamInfo;
import org.apache.curator.framework.CuratorFramework;

import java.util.Collection;

@Singleton
public class BaragonStateDatastore extends AbstractDataStore {
  public static final String SERVICES_FORMAT = "/singularity/state";
  public static final String SERVICE_FORMAT = "/singularity/state/%s";
  public static final String UPSTREAM_FORMAT = "/singularity/state/%s/%s";

  @Inject
  public BaragonStateDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Collection<String> getServices() {
    return getChildren(SERVICES_FORMAT);
  }

  public void addService(Service service) {
    writeToZk(String.format(SERVICE_FORMAT, service.getServiceId()), service);
  }

  public Optional<Service> getService(String serviceId) {
    return readFromZk(String.format(SERVICE_FORMAT, serviceId), Service.class);
  }

  public void removeService(String serviceId) {
    for (String upstream : getUpstreams(serviceId)) {
      deleteNode(String.format(UPSTREAM_FORMAT, serviceId, upstream));
    }

    deleteNode(String.format(SERVICE_FORMAT, serviceId));
  }

  public Collection<String> getUpstreams(String serviceId) {
    return getChildren(String.format(SERVICE_FORMAT, serviceId));
  }

  public Optional<UpstreamInfo> getUpstream(String serviceId, String upstream) {
    return readFromZk(String.format(UPSTREAM_FORMAT, serviceId, upstream), UpstreamInfo.class);
  }

  public void applyRequest(BaragonRequest request) {
    try {
      addService(request.getLoadBalancerService());
      final String serviceId = request.getLoadBalancerService().getServiceId();

      for (String remove : request.getRemoveUpstreams()) {
        deleteNode(String.format(UPSTREAM_FORMAT, serviceId, remove));
      }

      for (String add : request.getAddUpstreams()) {
        writeToZk(String.format(UPSTREAM_FORMAT, serviceId, add), new UpstreamInfo(add, request.getLoadBalancerRequestId()));
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

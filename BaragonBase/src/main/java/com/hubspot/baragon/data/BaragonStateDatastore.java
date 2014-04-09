package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.BaragonRequest;
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

  public void setService(Service service) {
    writeToZk(String.format(SERVICE_FORMAT, service.getId()), service);
  }

  public Optional<Service> getService(String serviceId) {
    return readFromZk(String.format(SERVICE_FORMAT, serviceId), Service.class);
  }

  public void removeService(String serviceId) {
    // TODO: recursive delete
    deleteNode(String.format(SERVICE_FORMAT, serviceId));
  }

  public Collection<String> getUpstreams(String serviceId) {
    return getChildren(String.format(SERVICE_FORMAT, serviceId));
  }

  public boolean addUpstream(String serviceId, String upstream) {
    return createNode(String.format(UPSTREAM_FORMAT, serviceId, upstream));
  }

  public boolean removeUpstream(String serviceId, String upstream) {
    return deleteNode(String.format(UPSTREAM_FORMAT, serviceId, upstream));
  }

  public void applyRequest(BaragonRequest request) {
    try {
      setService(request.getService());

      for (String remove : request.getRemove()) {
        deleteNode(String.format(UPSTREAM_FORMAT, request.getService().getId(), remove));
      }

      for (String add : request.getAdd()) {
        createNode(String.format(UPSTREAM_FORMAT, request.getService().getId(), add));
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}

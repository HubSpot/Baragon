package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;

public class ServiceManager {
  private final BaragonStateDatastore stateDatastore;
  private final RequestManager requestManager;

  @Inject
  public ServiceManager(BaragonStateDatastore stateDatastore, RequestManager requestManager) {
    this.stateDatastore = stateDatastore;
    this.requestManager = requestManager;
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

  public BaragonResponse enqueueReloadServiceConfigs(String serviceId) {
    String requestId = String.format("%s-%s-%s", serviceId, System.currentTimeMillis(), "RELOAD");
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (maybeService.isPresent()) {
      try {
        return requestManager.enqueueRequest(buildReloadRequest(maybeService.get(), requestId));
      } catch (Exception e) {
        return BaragonResponse.failure(requestId, e.getMessage());
      }
    } else {
      return BaragonResponse.serviceNotFound(requestId, serviceId);
    }
  }

  public BaragonResponse enqueueRemoveService(String serviceId) {
    String requestId = String.format("%s-%s-%s", serviceId, System.currentTimeMillis(), "DELETE");
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (maybeService.isPresent()) {
      try {
        return requestManager.enqueueRequest(buildRemoveRequest(maybeService.get(), requestId));
      } catch (Exception e) {
        return BaragonResponse.failure(requestId, e.getMessage());
      }
    } else {
      return BaragonResponse.serviceNotFound(requestId, serviceId);
    }
  }

  private BaragonRequest buildRemoveRequest(BaragonService service, String requestId) throws Exception {
    List<UpstreamInfo> empty = Collections.emptyList();
    List<UpstreamInfo> remove;
    remove =  new ArrayList<>(stateDatastore.getUpstreamsMap(service.getServiceId()).values());
    return new BaragonRequest(requestId, service, empty, remove, empty, Optional.<String>absent(), Optional.of(RequestAction.DELETE));
  }

  private BaragonRequest buildReloadRequest(BaragonService service, String requestId) {
    List<UpstreamInfo> empty = Collections.emptyList();
    return new BaragonRequest(requestId, service, empty, empty, empty, Optional.<String>absent(), Optional.of(RequestAction.RELOAD));
  }
}

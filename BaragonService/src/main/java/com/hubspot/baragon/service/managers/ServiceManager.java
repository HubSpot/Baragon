package com.hubspot.baragon.service.managers;

import java.util.ArrayList;
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

  public Optional<BaragonServiceState> getService(String serviceId) {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    try {
      return Optional.of(new BaragonServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreams(serviceId)));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public BaragonResponse enqueueReloadServiceConfigs(String serviceId, boolean noValidate) {
    String requestId = String.format("%s-%s-%s", serviceId, System.currentTimeMillis(), "RELOAD");
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (maybeService.isPresent()) {
      try {
        return requestManager.enqueueRequest(buildReloadRequest(maybeService.get(), requestId, noValidate));
      } catch (Exception e) {
        return BaragonResponse.failure(requestId, e.getMessage());
      }
    } else {
      return BaragonResponse.serviceNotFound(requestId, serviceId);
    }
  }

  public BaragonResponse enqueueRemoveService(String serviceId, boolean noValidate, boolean noReload) {
    String requestId = String.format("%s-%s-%s", serviceId, System.currentTimeMillis(), "DELETE");
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (maybeService.isPresent()) {
      try {
        return requestManager.enqueueRequest(buildRemoveRequest(maybeService.get(), requestId, noValidate, noReload));
      } catch (Exception e) {
        return BaragonResponse.failure(requestId, e.getMessage());
      }
    } else {
      return BaragonResponse.serviceNotFound(requestId, serviceId);
    }
  }

  private BaragonRequest buildRemoveRequest(BaragonService service, String requestId, boolean noValidate, boolean noReload) throws Exception {
    List<UpstreamInfo> empty = Collections.emptyList();
    List<UpstreamInfo> remove;
    remove =  new ArrayList<>(stateDatastore.getUpstreams(service.getServiceId()));
    return new BaragonRequest(requestId, service, empty, remove, empty, Optional.<String>absent(), Optional.of(RequestAction.DELETE), noValidate, noReload);
  }

  private BaragonRequest buildReloadRequest(BaragonService service, String requestId, boolean noValidate) {
    List<UpstreamInfo> empty = Collections.emptyList();
    return new BaragonRequest(requestId, service, empty, empty, empty, Optional.<String>absent(), Optional.of(RequestAction.RELOAD), noValidate, false);
  }
}

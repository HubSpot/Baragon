package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.service.models.ServiceState;
import com.hubspot.baragon.models.UpstreamInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public StateResource(BaragonStateDatastore stateDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.stateDatastore = stateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @GET
  public Collection<String> getServices() {
    return stateDatastore.getServices();
  }

  @GET
  @Path("/{serviceId}")
  public Optional<ServiceState> getService(@PathParam("serviceId") String serviceId) {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new ServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreams(serviceId)));
  }

  @DELETE
  @Path("/{serviceId}")
  public Optional<ServiceState> deleteService(@PathParam("serviceId") String serviceId) {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    stateDatastore.removeService(serviceId);
    for (String loadBalancerGroup : maybeServiceInfo.get().getLoadBalancerGroups()) {
      loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeServiceInfo.get().getServiceBasePath());
    }

    return Optional.of(new ServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreams(serviceId)));
  }

  @GET
  @Path("/{serviceId}/{upstream}")
  public Optional<UpstreamInfo> getUpstream(@PathParam("serviceId") String serviceId, @PathParam("upstream") String upstream) {
    return stateDatastore.getUpstream(serviceId, upstream);
  }
}

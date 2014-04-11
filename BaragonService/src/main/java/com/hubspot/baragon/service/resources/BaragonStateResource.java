package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceState;
import com.hubspot.baragon.models.UpstreamInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class BaragonStateResource {
  private final BaragonStateDatastore datastore;

  @Inject
  public BaragonStateResource(BaragonStateDatastore datastore) {
    this.datastore = datastore;
  }

  @GET
  public Collection<String> getServices() {
    return datastore.getServices();
  }

  @GET
  @Path("/{serviceId}")
  public Optional<ServiceState> getService(@PathParam("serviceId") String serviceId) {
    final Optional<Service> maybeServiceInfo = datastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new ServiceState(maybeServiceInfo.get(), datastore.getUpstreams(serviceId)));
  }

  @DELETE
  @Path("/{serviceId}")
  public Optional<ServiceState> deleteService(@PathParam("serviceId") String serviceId) {
    final Optional<Service> maybeServiceInfo = datastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    datastore.removeService(serviceId);

    return Optional.of(new ServiceState(maybeServiceInfo.get(), datastore.getUpstreams(serviceId)));
  }

  @GET
  @Path("/{serviceId}/{upstream}")
  public Optional<UpstreamInfo> getUpstream(@PathParam("serviceId") String serviceId, @PathParam("upstream") String upstream) {
    return datastore.getUpstream(serviceId, upstream);
  }
}

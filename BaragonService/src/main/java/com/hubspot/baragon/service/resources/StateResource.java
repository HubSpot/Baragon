package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.managers.ServiceManager;
import com.hubspot.baragon.models.BaragonServiceState;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final ServiceManager manager;

  @Inject
  public StateResource(ServiceManager manager) {
    this.manager = manager;
  }

  @GET
  public Collection<BaragonServiceState> getAllServices() {
    return manager.getAllServices();
  }

  @GET
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> getService(@PathParam("serviceId") String serviceId) {
    return manager.getService(serviceId);
  }

  @DELETE
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> removeService(@PathParam("serviceId") String serviceId) {
    return manager.removeService(serviceId);
  }
}

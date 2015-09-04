package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.managers.ServiceManager;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final ServiceManager serviceManager;

  @Inject
  public StateResource(ServiceManager serviceManager) {
    this.serviceManager = serviceManager;
  }

  @GET
  @NoAuth
  public Collection<BaragonServiceState> getAllServices() {
    return serviceManager.getAllServices();
  }

  @GET
  @NoAuth
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> getService(@PathParam("serviceId") String serviceId) {
    return serviceManager.getService(serviceId);
  }


  @POST
  @Path("/{serviceId}/reload")
  public BaragonResponse reloadConfigs(@PathParam("serviceId") String serviceId) {
    return serviceManager.enqueueReloadServiceConfigs(serviceId);
  }

  @DELETE
  @Path("/{serviceId}")
  public BaragonResponse removeService(@PathParam("serviceId") String serviceId) {
    return serviceManager.enqueueRemoveService(serviceId);
  }
}

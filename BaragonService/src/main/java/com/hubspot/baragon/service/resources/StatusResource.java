package com.hubspot.baragon.service.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.managers.StatusManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@NoAuth
public class StatusResource {
  private final StatusManager manager;

  @Inject
  public StatusResource(StatusManager manager) {
    this.manager = manager;
  }

  @GET
  public BaragonServiceStatus getServiceStatus() {
    return manager.getServiceStatus();
  }

  @GET
  @Path("/master")
  public BaragonServiceStatus getMasterServiceStatus() {
    return manager.getMasterServiceStatus();
  }
}

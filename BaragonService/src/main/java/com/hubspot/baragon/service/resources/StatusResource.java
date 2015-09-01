package com.hubspot.baragon.service.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.managers.StatusManager;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final StatusManager manager;

  @Inject
  public StatusResource(StatusManager manager) {
    this.manager = manager;

  }

  @GET
  @NoAuth
  public BaragonServiceStatus getServiceStatus() {
    return manager.getServiceStatus();
  }
}

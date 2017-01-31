package com.hubspot.baragon.auth;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import com.hubspot.baragon.managers.BaragonAuthManager;

public class BaragonAuthFilter implements ContainerRequestFilter {
  private final BaragonAuthManager authManager;

  @Inject
  public BaragonAuthFilter(BaragonAuthManager authManager) {
    this.authManager = authManager;
  }

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    String authKey = request.getUriInfo().getQueryParameters().getFirst("authkey");

    if (!authManager.isAuthenticated(authKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }
  }
}

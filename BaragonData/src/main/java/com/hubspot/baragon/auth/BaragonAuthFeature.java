package com.hubspot.baragon.auth;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.google.inject.Inject;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.managers.BaragonAuthManager;

@Provider
public class BaragonAuthFeature implements DynamicFeature {
  private final BaragonAuthFilter requestFilter;
  private final AuthConfiguration authConfiguration;


  public BaragonAuthFeature(BaragonAuthFilter requestFilter, AuthConfiguration authConfiguration) {
    this.requestFilter = requestFilter;
    this.authConfiguration = authConfiguration;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
    if (authConfiguration.isEnabled()) {
      if (resourceInfo.getResourceMethod().getAnnotation(NoAuth.class) == null) {
        featureContext.register(requestFilter);
      }
    }
  }

  public static class BaragonAuthFilter implements ContainerRequestFilter {
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
}

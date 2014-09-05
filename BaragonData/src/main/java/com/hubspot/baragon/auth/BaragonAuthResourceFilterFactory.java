package com.hubspot.baragon.auth;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.managers.BaragonAuthManager;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class BaragonAuthResourceFilterFactory implements ResourceFilterFactory {
  private final BaragonAuthFilter requestFilter;
  private final AuthConfiguration authConfiguration;

  @Inject
  public BaragonAuthResourceFilterFactory(BaragonAuthFilter requestFilter, AuthConfiguration authConfiguration) {
    this.requestFilter = requestFilter;
    this.authConfiguration = authConfiguration;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    if (authConfiguration.isEnabled()) {
      if ((am.getAnnotation(NoAuth.class) == null) && (am.getResource().getAnnotation(NoAuth.class) == null)) {
        return Collections.<ResourceFilter>singletonList(new BaragonAuthResourceFilter(requestFilter));
      }
    }

    return null;
  }

  public static class BaragonAuthResourceFilter implements ResourceFilter {
    private final ContainerRequestFilter requestFilter;

    public BaragonAuthResourceFilter(ContainerRequestFilter requestFilter) {
      this.requestFilter = requestFilter;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
      return requestFilter;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      return null;
    }
  }

  public static class BaragonAuthFilter implements ContainerRequestFilter {
    private final BaragonAuthManager authManager;

    @Inject
    public BaragonAuthFilter(BaragonAuthManager authManager) {
      this.authManager = authManager;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
      if (!authManager.isAuthenticated(request.getQueryParameters().getFirst("authkey"))) {
        throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
      }

      return request;
    }
  }
}

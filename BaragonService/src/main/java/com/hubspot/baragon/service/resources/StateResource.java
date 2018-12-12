package com.hubspot.baragon.service.resources;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.cache.BaragonStateCache;
import com.hubspot.baragon.cache.CachedBaragonState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import com.hubspot.baragon.service.managers.ServiceManager;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final ObjectMapper objectMapper;
  private final ServiceManager serviceManager;
  private final BaragonStateCache stateCache;

  @HeaderParam(HttpHeaders.ACCEPT_ENCODING)
  private String acceptEncoding;

  @Inject
  public StateResource(ObjectMapper objectMapper, ServiceManager serviceManager, BaragonStateCache stateCache) {
    this.objectMapper = objectMapper;
    this.serviceManager = serviceManager;
    this.stateCache = stateCache;
  }

  @GET
  @NoAuth
  @Timed
  public Response getAllServices() {
    CachedBaragonState state = stateCache.getState();

    ResponseBuilder builder = Response.ok();

    final byte[] entity;
    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
      builder.header(HttpHeaders.CONTENT_ENCODING, "gzip");
      entity = state.getGzip();
    } else {
      entity = state.getUncompressed();
    }

    return builder.entity(entity).build();
  }

  @GET
  @NoAuth
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> getService(@PathParam("serviceId") String serviceId) {
    return serviceManager.getService(serviceId);
  }

  @GET
  @NoAuth
  public List<BaragonServiceState> getService(@QueryParam("host") String host, @QueryParam("port") String port) {
    // Important - Normally, only a single service is on a given host and port. But in the past we've seen a critsit
    // where multiple services were on the same host and port. This endpoint returns a collection so we can easily
    // identify that scenario occur in the future.
    try {
      Collection<BaragonServiceState> services = objectMapper.readValue(stateCache.getState().getUncompressed(), new TypeReference<Collection<BaragonServiceState>>(){});
      return services.stream()
          .filter(state -> state.getUpstreams().stream()
              .anyMatch(u -> u.getUpstream().matches(String.format("%s\\.\\w+:%s", host, port))))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BaragonWebException("Failed to read service data.");
    }
  }

  @POST
  @Path("/{serviceId}/reload")
  public BaragonResponse reloadConfigs(@PathParam("serviceId") String serviceId, @DefaultValue("false") @QueryParam("noValidate") boolean noValidate) {
    return serviceManager.enqueueReloadServiceConfigs(serviceId, noValidate);
  }

  @DELETE
  @Path("/{serviceId}")
  public BaragonResponse removeService(@PathParam("serviceId") String serviceId, @DefaultValue("false") @QueryParam("noValidate") boolean noValidate, @DefaultValue("false") @QueryParam("noReload") boolean noReload) {
    return serviceManager.enqueueRemoveService(serviceId, noValidate, noReload);
  }
}

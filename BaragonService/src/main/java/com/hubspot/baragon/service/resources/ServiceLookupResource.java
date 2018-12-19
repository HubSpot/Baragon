package com.hubspot.baragon.service.resources;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.cache.BaragonStateCache;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.exceptions.BaragonWebException;

@Path("/service-lookup")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceLookupResource {
  private final ObjectMapper objectMapper;
  private final BaragonStateCache stateCache;

  @Inject
  public ServiceLookupResource(ObjectMapper objectMapper, BaragonStateCache stateCache) {
    this.objectMapper = objectMapper;
    this.stateCache = stateCache;
  }

  @GET
  @NoAuth
  public List<BaragonServiceState> getServices(@QueryParam("hostPort") String hostPort) {
    // Important - Normally, only a single service is on a given host and port. But in the past we've seen a critsit
    // where multiple services were on the same host and port. This endpoint returns a collection so we can easily
    // identify that scenario occur in the future.
    try {
      Collection<BaragonServiceState> services = objectMapper.readValue(stateCache.getState().getUncompressed(), new TypeReference<Collection<BaragonServiceState>>(){});
      return services.stream()
          .filter(state -> state.getUpstreams().stream()
              .anyMatch(u -> u.getUpstream().equals(hostPort)))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BaragonWebException("Failed to read service data.");
    }
  }
}

package com.hubspot.baragon.service.resources;

import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.managers.PurgeCacheManager;
import com.hubspot.horizon.HttpResponse;

@Path("/purgeCache")
@Produces(MediaType.APPLICATION_JSON)
public class PurgeCacheResource {

  private final PurgeCacheManager purgeCacheManager;

  @Inject
  public PurgeCacheResource(PurgeCacheManager purgeCacheManager){
    this.purgeCacheManager = purgeCacheManager;
  }

  @POST
  @NoAuth
  @Path("/{serviceId}")
  public List<HttpResponse> purgeCache(@PathParam("serviceId") String serviceId) throws Exception {
    return purgeCacheManager.synchronouslyRequestCachePurge(serviceId);
  }
}

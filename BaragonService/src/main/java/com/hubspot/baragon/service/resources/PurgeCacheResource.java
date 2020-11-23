package com.hubspot.baragon.service.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.service.managers.PurgeCacheManager;
import com.hubspot.baragon.service.managers.ServiceManager;

@Path("/purgeCache")
@Produces(MediaType.APPLICATION_JSON)
public class PurgeCacheResource {

  private final PurgeCacheManager purgeCacheManager;
  private final ServiceManager serviceManager;

  @Inject
  public PurgeCacheResource(PurgeCacheManager purgeCacheManager,
                            ServiceManager serviceManager){
    this.purgeCacheManager = purgeCacheManager;
    this.serviceManager = serviceManager;
  }

  @POST
  @Path("/{serviceId}")
  public BaragonResponse purgeCacheAsync(@PathParam("serviceId") String serviceId) {
    return serviceManager.enqueuePurgeCache(serviceId);
  }
}

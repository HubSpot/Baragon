package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceManager;

@Path("/upstreams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaragonUpstreamsResource {
 private final BaragonServiceManager baragonDeployManager;
  
  @Inject
  public BaragonUpstreamsResource(BaragonServiceManager baragonDeployManager) {
    this.baragonDeployManager = baragonDeployManager;
  }
  
  @GET
  @Path("/{serviceName}/{serviceId}/healthy")
  public Collection<String> getHealthyUpstreams(@PathParam("serviceName") String serviceName, @PathParam("serviceId") String serviceId) {
    return baragonDeployManager.getHealthyUpstreams(serviceName, serviceId);
  }

  @GET
  @Path("/{serviceName}/{serviceId}/unhealthy")
  public Collection<String> getUnhealthyUpstreams(@PathParam("serviceName") String serviceName, @PathParam("serviceId") String serviceId) {
    return baragonDeployManager.getUnhealthyUpstreams(serviceName, serviceId);
  }

  @POST
  @Path("/{serviceName}/{serviceId}")
  public void addUpstream(@PathParam("serviceName") String serviceName, @PathParam("serviceId") String serviceId, @QueryParam("upstream") String upstream, @QueryParam("healthy") @DefaultValue("false") Boolean healthy) {
    baragonDeployManager.addUnhealthyUpstream(serviceName, serviceId, upstream);

    if (healthy) {
      baragonDeployManager.makeUpstreamHealthy(serviceName, serviceId, upstream);
    }
  }

  @DELETE
  @Path("/{serviceName}/{serviceId}/{upstream}")
  public void removeUpstream(@PathParam("serviceName") String serviceName, @PathParam("serviceId") String serviceId, @PathParam("upstream") String upstream) {
    baragonDeployManager.removeUpstream(serviceName, serviceId, upstream);
  }
}
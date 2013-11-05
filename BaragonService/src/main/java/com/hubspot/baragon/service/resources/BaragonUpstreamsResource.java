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
  @Path("/{serviceName}/healthy")
  public Collection<String> getHealthyUpstreams(@PathParam("serviceName") String serviceName) {
    return baragonDeployManager.getHealthyUpstreams(serviceName);
  }

  @GET
  @Path("/{serviceName}/unhealthy")
  public Collection<String> getUnhealthyUpstreams(@PathParam("serviceName") String serviceName) {
    return baragonDeployManager.getUnhealthyUpstreams(serviceName);
  }

  @POST
  @Path("/{serviceName}/pending")
  public void addPendingUpstream(@PathParam("serviceName") String serviceName, @QueryParam("upstream") String upstream) {
    baragonDeployManager.addPendingUpstream(serviceName, upstream);
  }

  @POST
  @Path("/{serviceName}/active")
  public void addActiveUpstream(@PathParam("serviceName") String serviceName, @QueryParam("upstream") String upstream, @QueryParam("healthy") @DefaultValue("false") Boolean healthy) {
    baragonDeployManager.addActiveUpstream(serviceName, upstream);

    if (healthy) {
      baragonDeployManager.markUpstreamHealthy(serviceName, upstream);
    }
  }
}
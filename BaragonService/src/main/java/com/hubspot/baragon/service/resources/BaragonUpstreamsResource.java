package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
  public void addActiveUpstream(@PathParam("serviceName") String serviceName, @QueryParam("upstream") String upstream) {
    baragonDeployManager.addPendingUpstream(serviceName, upstream);
  }
}
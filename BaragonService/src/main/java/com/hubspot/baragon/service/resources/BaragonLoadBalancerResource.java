package com.hubspot.baragon.service.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaragonLoadBalancerResource {
  private final BaragonLoadBalancerDatastore datastore;

  @Inject
  public BaragonLoadBalancerResource(BaragonLoadBalancerDatastore datastore) {
    this.datastore = datastore;
  }

  @GET
  public Collection<String> getClusters() {
    return datastore.getClusters();
  }
  
  @GET
  @Path("/{clusterName}/hosts")
  public Collection<String> getHosts(@PathParam("clusterName") String clusterName) {
    return datastore.getHosts(clusterName);
  }
}

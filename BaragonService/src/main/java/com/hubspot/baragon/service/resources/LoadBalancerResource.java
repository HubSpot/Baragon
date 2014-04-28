package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoadBalancerResource {
  private final BaragonLoadBalancerDatastore datastore;

  @Inject
  public LoadBalancerResource(BaragonLoadBalancerDatastore datastore) {
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

  @GET
  @Path("/{clusterName}/base-uri")
  public Optional<String> getBaseUri(@PathParam("clusterName") String clusterName, @QueryParam("baseUri") String baseUri) {
    return datastore.getBaseUriServiceId(clusterName, baseUri);
  }

  @DELETE
  @Path("/{clusterName}/base-uri")
  public void clearBaseUri(@PathParam("clusterName") String clusterName, @QueryParam("baseUri") String baseUri) {
    datastore.clearBaseUri(clusterName, baseUri);
  }
}

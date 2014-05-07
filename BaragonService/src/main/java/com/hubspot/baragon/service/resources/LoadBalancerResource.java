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
    return datastore.getBaseUrls(clusterName);
  }

  @GET
  @Path("/{clusterName}/base-path/all")
  public Collection<String> getBasePaths(@PathParam("clusterNAme") String clusterName) {
    return datastore.getBasePaths(clusterName);
  }

  @GET
  @Path("/{clusterName}/base-path")
  public Optional<String> getBasePathServiceId(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    return datastore.getBasePathServiceId(clusterName, basePath);
  }

  @DELETE
  @Path("/{clusterName}/base-path")
  public void clearBasePath(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    datastore.clearBasePath(clusterName, basePath);
  }
}

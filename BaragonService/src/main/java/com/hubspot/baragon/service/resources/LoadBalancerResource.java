package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoadBalancerResource {
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonStateDatastore stateDatastore;

  @Inject
  public LoadBalancerResource(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              BaragonStateDatastore stateDatastore) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.stateDatastore = stateDatastore;
  }

  @GET
  public Collection<String> getClusters() {
    return loadBalancerDatastore.getClusters();
  }
  
  @GET
  @Path("/{clusterName}/hosts")
  public Collection<String> getHosts(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getBaseUrls(clusterName);
  }

  @GET
  @Path("/{clusterName}/base-path/all")
  public Collection<String> getBasePaths(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getBasePaths(clusterName);
  }

  @GET
  @Path("/{clusterName}/base-path")
  public Optional<Service> getBasePathServiceId(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(clusterName, basePath);

    if (!maybeServiceId.isPresent()) {
      return Optional.absent();
    }

    return stateDatastore.getService(maybeServiceId.get());
  }

  @DELETE
  @Path("/{clusterName}/base-path")
  public void clearBasePath(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    loadBalancerDatastore.clearBasePath(clusterName, basePath);
  }
}

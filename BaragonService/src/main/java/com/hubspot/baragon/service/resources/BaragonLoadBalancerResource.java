package com.hubspot.baragon.service.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LoadBalancerManager;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaragonLoadBalancerResource {
  private final LoadBalancerManager manager;
  
  @Inject
  public BaragonLoadBalancerResource(LoadBalancerManager manager) {
    this.manager = manager;
  }

  @GET
  public List<String> getLoadBalancers() {
    return manager.getLoadBalancers();
  }
  
  @GET
  @Path("/{loadBalancer}/hosts")
  public List<String> getCluster(@PathParam("loadBalancer") String loadBalancer) {
    return manager.getLoadBalancerHosts(loadBalancer);
  }
  
  @GET
  @Path("/{loadBalancer}/status")
  public Map<String, Boolean> getClusterStatus(@PathParam("loadBalancer") String loadBalancer) {
    return manager.checkConfigs(loadBalancer);
  }
}

package com.hubspot.baragon.service.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.google.inject.Inject;
import com.hubspot.baragon.service.exceptions.BaragonNotFoundException;
import com.hubspot.baragon.service.exceptions.BaragonWebException;

@Path("/elb")
public class ElbResource {
  private final AmazonElasticLoadBalancingClient elbClient;

  @Inject
  public ElbResource(AmazonElasticLoadBalancingClient elbClient) {
    this.elbClient = elbClient;
  }

  @GET
  public List<LoadBalancerDescription> getAllElbs() {
    return elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
  }

  @GET
  @Path("/{elbName}")
  public LoadBalancerDescription getElb(@PathParam("elbName") String elbName) {
    try {
      for (LoadBalancerDescription elb : elbClient.describeLoadBalancers().getLoadBalancerDescriptions()) {
        if (elb.getLoadBalancerName().equals(elbName)) {
          return elb;
        }
      }
    } catch (AmazonClientException e) {
      throw new BaragonWebException(String.format("AWS Client Error: %s", e));
    }
    throw new BaragonNotFoundException(String.format("Elb with name %s not found", elbName));
  }
}

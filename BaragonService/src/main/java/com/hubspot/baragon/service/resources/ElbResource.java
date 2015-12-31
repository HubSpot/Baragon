package com.hubspot.baragon.service.resources;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.cache.ElbCache;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonNotFoundException;
import com.hubspot.baragon.service.exceptions.BaragonWebException;

@Path("/elbs")
@Produces(MediaType.APPLICATION_JSON)
public class ElbResource {
  private final Optional<ElbConfiguration> config;
  private final ElbCache elbCache;

  @Inject
  public ElbResource(Optional<ElbConfiguration> config,
                     ElbCache elbCache) {
    this.config = config;
    this.elbCache = elbCache;
  }


  @GET
  @NoAuth
  public List<LoadBalancerDescription> getAllElbs() {
    if (config.isPresent()) {
      return elbCache.getAllDescriptions();
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/{elbName}")
  public LoadBalancerDescription getElb(@PathParam("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        Optional<LoadBalancerDescription> maybeLoadBalancerDescription = elbCache.getElbInfo(elbName);
        if (!maybeLoadBalancerDescription.isPresent()) {
          throw new BaragonNotFoundException(String.format("ELB with name %s not found", elbName));
        } else {
          return maybeLoadBalancerDescription.get();
        }
      } catch (AmazonClientException e) {
        throw new BaragonWebException(String.format("AWS Client Error: %s", e));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/{elbName}/update")
  public RegisterInstancesWithLoadBalancerResult addToElb(@PathParam("elbName") String elbName, @QueryParam("instanceId") String instanceId) {
    if (config.isPresent()) {
      RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Collections.singletonList(new Instance(instanceId)));
      return elbCache.addToElb(request);
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/{elbName}/update")
  public DeregisterInstancesFromLoadBalancerResult removeFromElb(@PathParam("elbName") String elbName, @QueryParam("instanceId") String instanceId) {
    if (config.isPresent()) {
      DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(elbName, Collections.singletonList(new Instance(instanceId)));
      return elbCache.removeFromElb(request);
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/cache")
  public void clearCache() {
    elbCache.clear();
  }
}

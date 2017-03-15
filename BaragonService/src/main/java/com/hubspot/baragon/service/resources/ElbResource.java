package com.hubspot.baragon.service.resources;

import java.util.Arrays;
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
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonNotFoundException;
import com.hubspot.baragon.service.exceptions.BaragonWebException;

@Path("/elbs")
@Produces(MediaType.APPLICATION_JSON)
public class ElbResource {
  private final AmazonElasticLoadBalancingClient elbClient;
  private final Optional<ElbConfiguration> config;

  @Inject
  public ElbResource(@Named(BaragonServiceModule.BARAGON_AWS_ELB_CLIENT_V1) AmazonElasticLoadBalancingClient elbClient,
                     Optional<ElbConfiguration> config) {
    this.elbClient = elbClient;
    this.config = config;
  }


  @GET
  @NoAuth
  public List<LoadBalancerDescription> getAllElbs() {
    if (config.isPresent()) {
      return elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
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
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(Arrays.asList(elbName));
        DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
        for (LoadBalancerDescription elb : result.getLoadBalancerDescriptions()) {
          if (elb.getLoadBalancerName().equals(elbName)) {
            return elb;
          }
        }
      } catch (AmazonClientException e) {
        throw new BaragonWebException(String.format("AWS Client Error: %s", e));
      }
      throw new BaragonNotFoundException(String.format("ELB with name %s not found", elbName));
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/{elbName}/instances")
  public List<InstanceState> getInstancesByElb(@PathParam("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest(elbName);
        DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(request);
        return result.getInstanceStates();
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client Error %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/{elbName}/update")
  public RegisterInstancesWithLoadBalancerResult addToElb(@PathParam("elbName") String elbName, @QueryParam("instanceId") String instanceId) {
    if (config.isPresent()) {
      RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(elbName, Arrays.asList(new Instance(instanceId)));
      return elbClient.registerInstancesWithLoadBalancer(request);
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/{elbName}/update")
  public DeregisterInstancesFromLoadBalancerResult removeFromElb(@PathParam("elbName") String elbName, @QueryParam("instanceId") String instanceId) {
    if (config.isPresent()) {
      DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(elbName, Arrays.asList(new Instance(instanceId)));
      return elbClient.deregisterInstancesFromLoadBalancer(request);
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }
}

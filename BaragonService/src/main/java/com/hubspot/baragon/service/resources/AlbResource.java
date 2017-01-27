package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.elb.ApplicationLoadBalancer;
import com.hubspot.baragon.service.elb.RegisterInstanceResult;
import com.hubspot.baragon.service.exceptions.BaragonWebException;

@Path("/albs")
@Produces(MediaType.APPLICATION_JSON)
public class AlbResource {
  private final ApplicationLoadBalancer applicationLoadBalancer;
  private final Optional<ElbConfiguration> config;

  @Inject
  public AlbResource(ApplicationLoadBalancer applicationLoadBalancer,
                     Optional<ElbConfiguration> config) {
    this.applicationLoadBalancer = applicationLoadBalancer;
    this.config = config;
  }

  @GET
  @NoAuth
  @Path("/load-balancers")
  public Collection<LoadBalancer> getAllLoadBalancers() {
    if (config.isPresent()) {
      return applicationLoadBalancer.getAllLoadBalancers();
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/load-balancers/{elbName}")
  public LoadBalancer getLoadBalancer(@PathParam("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        Optional<LoadBalancer> maybeLoadBalancer = applicationLoadBalancer.getLoadBalancer(elbName);
        if (maybeLoadBalancer.isPresent()) {
          return maybeLoadBalancer.get();
        } else {
          throw new BaragonWebException(String.format("ALB with name %s not found", elbName));
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/target-groups")
  public Collection<TargetGroup> getAllTargetGroups() {
    if (config.isPresent()) {
      return applicationLoadBalancer.getAllTargetGroups();
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/target-groups/{targetGroup}")
  public TargetGroup getTargetGroup(@PathParam("targetGroup") String targetGroup) {
    if (config.isPresent()) {
      try {
        Optional<TargetGroup> maybeTargetGroup = applicationLoadBalancer.getTargetGroup(targetGroup);
        if (maybeTargetGroup.isPresent()) {
          return maybeTargetGroup.get();
        } else {
          throw new BaragonWebException(String.format("TargetGroup with name %s not found", targetGroup));
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/target-groups/{targetGroup}/targets")
  public Collection<TargetDescription> getTargets(@PathParam("targetGroup") String targetGroup) {
    if (config.isPresent()) {
      try {
        Optional<TargetGroup> maybeTargetGroup = applicationLoadBalancer.getTargetGroup(targetGroup);
        if (maybeTargetGroup.isPresent()) {
          return applicationLoadBalancer.getTargetsOn(maybeTargetGroup.get());
        } else {
          throw new BaragonWebException(String.format("TargetGroup with name %s not found", targetGroup));
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/target-groups/{targetGroup}/update")
  public DeregisterTargetsResult removeFromTargetGroup(@PathParam("targetGroup") String targetGroup,
                                                       @QueryParam("instanceId") String instanceId) {
    if (instanceId == null) {
      throw new BaragonWebException("Must provide instance ID to remove target from group");
    } else if (config.isPresent()) {
      Optional<DeregisterTargetsResult> maybeResult = applicationLoadBalancer.removeInstance(instanceId, targetGroup);
      if (maybeResult.isPresent()) {
        return maybeResult.get();
      } else {
        throw new BaragonWebException(String.format("No instance with ID %s could be found", instanceId));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions not currently enabled");
    }
  }

  @POST
  @Path("/target-group/{targetGroup}/update")
  public RegisterInstanceResult addToTargetGroup(@PathParam("targetGroup") String targetGroup,
                                                 @QueryParam("instanceId") String instanceId) {
    if (instanceId == null) {
      throw new BaragonWebException("Must provide instance ID to add target to group");
    } else if (config.isPresent()) {
      try {
        return applicationLoadBalancer.registerInstance(instanceId, targetGroup);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("Failed to add instance %s to target group %s", instanceId, targetGroup));
      }

    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }
}

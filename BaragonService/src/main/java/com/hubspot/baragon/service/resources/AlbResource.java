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
import javax.ws.rs.core.Response;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.ListenerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Rule;
import com.amazonaws.services.elasticloadbalancingv2.model.RuleNotFoundException;
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
  @Path("/load-balancers/{elbName}/listeners")
  public Collection<Listener> getListeners(@PathParam("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.getListenersForElb(elbName);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/load-balancers/{elbName}/listeners")
  public Listener createListeners(@PathParam("elbName") String elbName,
                                  CreateListenerRequest createListenerRequest) {
    if (config.isPresent()) {
        Optional<LoadBalancer> maybeLoadBalancer = applicationLoadBalancer
            .getLoadBalancer(elbName);
        if (maybeLoadBalancer.isPresent()) {
          return applicationLoadBalancer
              .createListener(createListenerRequest
                  .withLoadBalancerArn(maybeLoadBalancer.get().getLoadBalancerArn()));
        } else {
          throw new BaragonWebException(String.format("Could not find an elb with name %s", elbName));
        }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}")
  public Listener updateListener(@PathParam("elbName") String elbName,
                                 @PathParam("listenerArn") String listenerArn,
                                 ModifyListenerRequest modifyListenerRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .modifyListener(modifyListenerRequest.withListenerArn(listenerArn));
      } catch (ListenerNotFoundException notFound) {
        throw new BaragonWebException(String.format("No listener with ARN %s found", listenerArn));
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}")
  public Response removeListener(@PathParam("elbName") String elbName,
                                 @PathParam("listenerArn") String listenerArn) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteListener(listenerArn);
        return Response.noContent().build();
      } catch (ListenerNotFoundException notFound) {
        throw new BaragonWebException(String.format("No listener with ARN %s found", listenerArn));
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}/rules")
  public Collection<Rule> getRules(@PathParam("elbName") String elbName, @PathParam("listenerArn") String listenerArn) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.getRulesByListener(listenerArn);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}/rules")
  public Rule createRule(@PathParam("elbName") String elbName,
                         @PathParam("listenerArn") String listenerArn,
                         CreateRuleRequest createRuleRequest) {
    if (config.isPresent()) {
      return applicationLoadBalancer
          .createRule(createRuleRequest
              .withListenerArn(listenerArn));
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}/rules/{ruleArn}")
  public Rule updateRule(@PathParam("elbName") String elbName,
                         @PathParam("listenerArn") String listenerArn,
                         @PathParam("ruleArn") String ruleArn,
                         ModifyRuleRequest modifyRuleRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .modifyRule(modifyRuleRequest.withRuleArn(ruleArn));
      } catch (RuleNotFoundException notFound) {
        throw new BaragonWebException(String.format("Rule with ARN %s found", ruleArn));
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/load-balancers/{elbName}/listeners/{listenerArn}/rules/{ruleArn}")
  public Response deleteRule(@PathParam("elbName") String elbName,
                             @PathParam("listenerArn") String listenerArn,
                             @PathParam("ruleArn") String ruleArn) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteRule(ruleArn);
        return Response.noContent().build();
      } catch (RuleNotFoundException notFound) {
        throw new BaragonWebException(String.format("Rule with ARN %s not found", ruleArn));
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("Amazon client exception %s", exn));
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

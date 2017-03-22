package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.ListenerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Rule;
import com.amazonaws.services.elasticloadbalancingv2.model.RuleNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.elb.ApplicationLoadBalancer;
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
          throw new WebApplicationException(String.format("ELB %s not found", elbName), Status.NOT_FOUND);
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
                                  @Valid CreateListenerRequest createListenerRequest) {
    if (config.isPresent()) {
        Optional<LoadBalancer> maybeLoadBalancer = applicationLoadBalancer
            .getLoadBalancer(elbName);
        if (maybeLoadBalancer.isPresent()) {
          return applicationLoadBalancer
              .createListener(createListenerRequest
                  .withLoadBalancerArn(maybeLoadBalancer.get().getLoadBalancerArn()));
        } else {
          throw new WebApplicationException(String.format("Could not find an elb with name %s", elbName), Status.NOT_FOUND);
        }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/listeners/{listenerArn:.+}")
  public Listener updateListener(@PathParam("elbName") String elbName,
                                 @PathParam("listenerArn") String listenerArn,
                                 @Valid ModifyListenerRequest modifyListenerRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .modifyListener(modifyListenerRequest.withListenerArn(listenerArn));
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(String.format("No listener with ARN %s found", listenerArn), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/listeners/{listenerArn:.+}")
  public Response removeListener(@PathParam("elbName") String elbName,
                                 @PathParam("listenerArn") String listenerArn) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteListener(listenerArn);
        return Response.noContent().build();
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(String.format("No listener with ARN %s found", listenerArn), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/listeners/rules/{listenerArn:.+}")
  public Collection<Rule> getRules(@PathParam("elbName") String elbName,
                                   @PathParam("listenerArn") String listenerArn) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.getRulesByListener(listenerArn);
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(String.format("Listener with ARN %s not found", listenerArn), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/listeners/rules/{listenerArn:.+}")
  public Rule createRule(@PathParam("elbName") String elbName,
                         @PathParam("listenerArn") String listenerArn,
                         @Valid CreateRuleRequest createRuleRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .createRule(createRuleRequest
                .withListenerArn(listenerArn));
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(String.format("Listener with ARN %s not found", listenerArn), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/rules/{ruleArn:.+}")
  public Rule updateRule(@PathParam("elbName") String elbName,
                         @PathParam("listenerArn") String listenerArn,
                         @PathParam("ruleArn") String ruleArn,
                         @Valid ModifyRuleRequest modifyRuleRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .modifyRule(modifyRuleRequest.withRuleArn(ruleArn));
      } catch (RuleNotFoundException notFound) {
        throw new WebApplicationException(String.format("Rule with ARN %s found", ruleArn), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/rules/{ruleArn:.+}")
  public Response deleteRule(@PathParam("elbName") String elbName,
                             @PathParam("listenerArn") String listenerArn,
                             @PathParam("ruleArn") String ruleArn) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteRule(ruleArn);
        return Response.noContent().build();
      } catch (RuleNotFoundException notFound) {
        throw new WebApplicationException(String.format("Rule with ARN %s not found", ruleArn), Status.NOT_FOUND);
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

  @POST
  @PathParam("/target-groups")
  public TargetGroup createTargetGroup(@Valid CreateTargetGroupRequest createTargetGroupRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.createTargetGroup(createTargetGroupRequest);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
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
          throw new WebApplicationException(String.format("TargetGroup with name %s not found", targetGroup), Status.NOT_FOUND);
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @POST
  @Path("/target-groups/{targetGroup}")
  public TargetGroup modifyTargetGroup(@PathParam("targetGroup") String targetGroup,
                                       @Valid ModifyTargetGroupRequest modifyTargetGroupRequest) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer
            .modifyTargetGroup(modifyTargetGroupRequest);
      } catch (TargetGroupNotFoundException notFound) {
        throw new WebApplicationException(String.format("Target group %s not found", targetGroup), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @GET
  @NoAuth
  @Path("/target-groups/{targetGroup}/targets")
  public Collection<TargetHealthDescription> getTargets(@PathParam("targetGroup") String targetGroup) {
    if (config.isPresent()) {
      try {
        Optional<TargetGroup> maybeTargetGroup = applicationLoadBalancer.getTargetGroup(targetGroup);
        if (maybeTargetGroup.isPresent()) {
          return applicationLoadBalancer.getTargetsOn(maybeTargetGroup.get());
        } else {
          throw new WebApplicationException(String.format("TargetGroup with name %s not found", targetGroup), Status.NOT_FOUND);
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }

  @DELETE
  @Path("/target-groups/{targetGroup}/targets/{instanceId}")
  public AgentCheckInResponse removeFromTargetGroup(@PathParam("targetGroup") String targetGroup,
                                                    @PathParam("instanceId") String instanceId) {
    if (instanceId == null) {
      throw new BaragonWebException("Must provide instance ID to remove target from group");
    } else if (config.isPresent()) {
      AgentCheckInResponse result = applicationLoadBalancer.removeInstance(instanceId, targetGroup);
      if (result.getExceptionMessage().isPresent()) {
        throw new WebApplicationException(result.getExceptionMessage().get(), Status.INTERNAL_SERVER_ERROR);
      }
      return result;
    } else {
      throw new BaragonWebException("ElbSync and related actions not currently enabled");
    }
  }

  @POST
  @Path("/target-group/{targetGroup}/targets")
  public AgentCheckInResponse addToTargetGroup(@PathParam("targetGroup") String targetGroup,
                                                 @QueryParam("instanceId") String instanceId) {
    if (instanceId == null) {
      throw new BaragonWebException("Must provide instance ID to add target to group");
    } else if (config.isPresent()) {
      try {
        return applicationLoadBalancer.registerInstance(instanceId, targetGroup);
      } catch (TargetGroupNotFoundException notFound) {
        throw new WebApplicationException(String.format("Target group %s not found", targetGroup), Status.NOT_FOUND);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("Failed to add instance %s to target group %s", instanceId, targetGroup));
      }

    } else {
      throw new BaragonWebException("ElbSync and related actions are not currently enabled");
    }
  }
}

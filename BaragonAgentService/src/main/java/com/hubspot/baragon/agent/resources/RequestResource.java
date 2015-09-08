package com.hubspot.baragon.agent.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.models.RequestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final AgentRequestManager agentRequestManager;
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public RequestResource(AgentRequestManager agentRequestManager,
                         LoadBalancerConfiguration loadBalancerConfiguration) {
    this.agentRequestManager = agentRequestManager;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  @POST
  public Response apply(@PathParam("requestId") String requestId) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, Optional.<RequestAction>absent());
  }

  @DELETE
  public Response revert(@PathParam("requestId") String requestId) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, Optional.of(RequestAction.REVERT));
  }

}

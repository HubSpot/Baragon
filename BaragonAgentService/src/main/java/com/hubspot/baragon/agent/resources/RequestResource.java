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
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.RequestAction;


@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private final AgentRequestManager agentRequestManager;

  @Inject
  public RequestResource(AgentRequestManager agentRequestManager) {
    this.agentRequestManager = agentRequestManager;
  }

  @POST
  @Path("/literal")
  public Response applyLiteral(@PathParam("requestId") String requestId, BaragonRequest baragonRequest) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, RequestAction.UPDATE, baragonRequest, Optional.absent(), false, Optional.absent());
  }

  @POST
  public Response apply(@PathParam("requestId") String requestId) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, Optional.<RequestAction>absent(), false, Optional.<Integer>absent());
  }

  @DELETE
  public Response revert(@PathParam("requestId") String requestId) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, Optional.of(RequestAction.REVERT), false, Optional.<Integer>absent());
  }

}

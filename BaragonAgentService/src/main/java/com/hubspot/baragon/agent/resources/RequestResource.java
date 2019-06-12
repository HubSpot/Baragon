package com.hubspot.baragon.agent.resources;

import java.util.Collections;

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
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.RequestAction;


@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private final AgentRequestManager agentRequestManager;
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;

  @Inject
  public RequestResource(AgentRequestManager agentRequestManager, BaragonRequestDatastore requestDatastore, BaragonStateDatastore stateDatastore) {
    this.agentRequestManager = agentRequestManager;
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
  }

  @POST
  @Path("/literal")
  public Response applyLiteral(@PathParam("requestId") String requestId, BaragonRequest baragonRequest) throws InterruptedException {
    return agentRequestManager.processRequest(requestId, RequestAction.UPDATE, baragonRequest, Optional.absent(), Collections.emptyMap(), false, Optional.absent());
  }

  @POST
  public Response apply(@PathParam("requestId") String requestId) throws Exception {
    Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }

    String serviceId = maybeRequest.get().getLoadBalancerService().getServiceId();

    return agentRequestManager.processRequest(
        requestId,
        Collections.singletonMap(serviceId, stateDatastore.getUpstreams(serviceId)),
        Collections.singletonMap(serviceId, stateDatastore.getService(serviceId)),
        Collections.singletonMap(requestId, maybeRequest),
        Optional.<RequestAction>absent(), false, Optional.<Integer>absent()
    );
  }

  @DELETE
  public Response revert(@PathParam("requestId") String requestId) throws Exception {
    Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }

    String serviceId = maybeRequest.get().getLoadBalancerService().getServiceId();

    return agentRequestManager.processRequest(
        requestId,
        Collections.singletonMap(serviceId, stateDatastore.getUpstreams(serviceId)),
        Collections.singletonMap(serviceId, stateDatastore.getService(serviceId)),
        Collections.singletonMap(requestId, maybeRequest),
        Optional.of(RequestAction.REVERT), false, Optional.<Integer>absent()
    );
  }

}

package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.exceptions.BasePathConflictException;
import com.hubspot.baragon.exceptions.MissingLoadBalancerGroupException;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Log LOG = LogFactory.getLog(RequestResource.class);

  private final RequestManager manager;

  @Inject
  public RequestResource(RequestManager manager) {
    this.manager = manager;
  }

  @GET
  @Path("/{requestId}")
  public Optional<BaragonResponse> getResponse(@PathParam("requestId") String requestId) {
    return manager.getResponse(requestId);
  }

  @POST
  public BaragonResponse enqueueRequest(@Valid BaragonRequest request) {
    try {
      LOG.info(String.format("Received request: %s", request));
      return manager.enqueueRequest(request);
    } catch (BasePathConflictException e) {
      LOG.warn(String.format("Base path conflict for request %s (original service id: %s)", request.getLoadBalancerRequestId(), e.getOriginalServiceId()), e);
      throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
          .entity(BaragonResponse.failure(request.getLoadBalancerRequestId(), e.getMessage()))
          .build());
    } catch (MissingLoadBalancerGroupException e) {
      LOG.warn(String.format("Missing load balancer(s) for request %s", request.getLoadBalancerRequestId()), e);
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(BaragonResponse.failure(request.getLoadBalancerRequestId(), e.getMessage()))
          .build());
    }
  }

  @DELETE
  @Path("/{requestId}")
  public Optional<BaragonResponse> cancelRequest(@PathParam("requestId") String requestId) {
    // prevent race conditions when transitioning from a cancel-able to not cancel-able state
    synchronized (BaragonRequestWorker.class) {
      manager.cancelRequest(requestId);
      return manager.getResponse(requestId);
    }
  }
}

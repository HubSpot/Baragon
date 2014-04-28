package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
  private final BaragonRequestDatastore datastore;

  @Inject
  public RequestResource(BaragonRequestDatastore datastore) {
    this.datastore = datastore;
  }

  @GET
  @Path("/{requestId}")
  public Optional<BaragonResponse> getRequest(@PathParam("requestId") String requestId) {
    return datastore.getResponse(requestId);
  }

  @POST
  public BaragonResponse enqueueRequest(@Valid BaragonRequest request) {
    final Optional<BaragonResponse> maybeResponse = datastore.getResponse(request.getLoadBalancerRequestId());

    if (maybeResponse.isPresent()) {
      return maybeResponse.get();
    } else {
      return datastore.addPendingRequest(request);
    }
  }

  @DELETE
  @Path("/{requestId}")
  public Optional<BaragonResponse> cancelRequest(@PathParam("requestId") String requestId) {
    return datastore.cancelRequest(requestId);
  }
}

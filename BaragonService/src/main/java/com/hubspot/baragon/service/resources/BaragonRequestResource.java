package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.BaragonValidator;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.RequestState;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Queue;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class BaragonRequestResource {
  private final BaragonRequestDatastore datastore;
  private final Queue<BaragonRequest> queue;

  @Inject
  public BaragonRequestResource(BaragonRequestDatastore datastore,
                                @Named(BaragonServiceModule.BARAGON_SERVICE_QUEUE) Queue<BaragonRequest> queue) {
    this.datastore = datastore;
    this.queue = queue;
  }

  @GET
  @Path("/{requestId}")
  public Optional<BaragonResponse> getRequest(@PathParam("requestId") String requestId) {
    final Optional<RequestState> maybeState = datastore.getRequestState(requestId);

    if (maybeState.isPresent()) {
      return Optional.of(new BaragonResponse(requestId, maybeState.get()));
    } else {
      return Optional.absent();
    }
  }

  @POST
  public BaragonResponse addRequest(BaragonRequest request) {
    BaragonValidator.validateRequest(request);

    final Optional<RequestState> maybeState = datastore.getRequestState(request.getRequestId());

    if (maybeState.isPresent()) {
      return new BaragonResponse(request.getRequestId(), maybeState.get());
    }

    final BaragonResponse response = datastore.addRequest(request);

    queue.add(request);

    return response;
  }

  @DELETE
  @Path("/{requestId}")
  public Optional<BaragonResponse> cancelRequest(@PathParam("requestId") String requestId) {
    return datastore.cancelRequest(requestId);
  }
}

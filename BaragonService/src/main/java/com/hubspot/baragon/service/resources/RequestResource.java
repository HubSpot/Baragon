package com.hubspot.baragon.service.resources;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.baragon.service.history.RequestIdHistoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final RequestManager manager;
  private final ObjectMapper objectMapper;
  private final RequestIdHistoryHelper requestIdHistoryHelper;

  @Inject
  public RequestResource(RequestManager manager, ObjectMapper objectMapper, RequestIdHistoryHelper requestIdHistoryHelper) {
    this.manager = manager;
    this.objectMapper = objectMapper;
    this.requestIdHistoryHelper = requestIdHistoryHelper;
  }

  @GET
  @Path("/{requestId}")
  public Optional<BaragonResponse> getResponse(@PathParam("requestId") String requestId) {
    return requestIdHistoryHelper.getResponseById(requestId);
  }

  @POST
  public BaragonResponse enqueueRequest(@Valid BaragonRequest request) {
    try {
      LOG.info(String.format("Received request: %s", objectMapper.writeValueAsString(request)));
      return manager.enqueueRequest(request);
    } catch (Exception e) {
      LOG.error(String.format("Caught exception for %s", request.getLoadBalancerRequestId()), e);
      return BaragonResponse.failure(request.getLoadBalancerRequestId(), e.getMessage());
    }
  }

  @GET
  public List<QueuedRequestId> getQueuedRequestIds() {
    return manager.getQueuedRequestIds();
  }

  @GET
  @Path("/history")
  public List<String> getRecentRequestIds(@QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);
    return requestIdHistoryHelper.getBlendedHistory(limitStart, limitCount);
  }

  @GET
  @Path("/history/{serviceId}")
  public List<String> getRcentRequestIdsForService(@PathParam("serviceId") String serviceId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);
    return requestIdHistoryHelper.getBlendedHistory(Optional.of(serviceId), limitStart, limitCount);
  }

  @DELETE
  @Path("/{requestId}")
  public BaragonResponse cancelRequest(@PathParam("requestId") String requestId) {
    // prevent race conditions when transitioning from a cancel-able to not cancel-able state
    synchronized (BaragonRequestWorker.class) {
      manager.cancelRequest(requestId);
      return requestIdHistoryHelper.getResponseById(requestId).or(BaragonResponse.requestDoesNotExist(requestId));
    }
  }

  private Integer getLimitCount(Integer countParam) {
    if (countParam == null) {
      return 100;
    }

    if (countParam > 1000) {
      return 1000;
    }

    return countParam;
  }

  private Integer getLimitStart(Integer limitCount, Integer pageParam) {
    if (pageParam == null) {
      return 0;
    }

    return limitCount * (pageParam - 1);
  }
}

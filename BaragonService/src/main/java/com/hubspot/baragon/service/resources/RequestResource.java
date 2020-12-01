package com.hubspot.baragon.service.resources;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.managers.AliasManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.config.PurgeCacheConfiguration;
import com.hubspot.baragon.service.managers.PurgeCacheManager;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;

@Path("/request")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final RequestManager manager;
  private final AliasManager aliasManager;
  private final PurgeCacheManager purgeCacheManager;

  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore,
                         RequestManager manager,
                         AliasManager aliasManager,
                         BaragonLoadBalancerDatastore loadBalancerDatastore,
                         PurgeCacheManager purgeCacheManager) {
    this.stateDatastore = stateDatastore;
    this.manager = manager;
    this.aliasManager = aliasManager;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.purgeCacheManager = purgeCacheManager;
  }

  @GET
  @NoAuth
  @Path("/{requestId}")
  public Optional<BaragonResponse> getResponse(@PathParam("requestId") String requestId) {
    return manager.getResponse(requestId);
  }

  @POST
  public BaragonResponse enqueueRequest(@Valid BaragonRequest request) {
    try {
      BaragonRequest updatedForAliases = aliasManager.updateForAliases(request);
      BaragonRequest updatedForDefaultDomains = loadBalancerDatastore.updateForDefaultDomains(updatedForAliases);
      BaragonRequest updatedForPurgeCache = purgeCacheManager.updateForPurgeCache(updatedForDefaultDomains);
      LOG.info("Received request: {}", request);
      return manager.enqueueRequest(updatedForPurgeCache);
    } catch (Exception e) {
      LOG.error("Caught exception for {}", request.getLoadBalancerRequestId(), e);
      return BaragonResponse.failure(request.getLoadBalancerRequestId(), e.getMessage());
    }
  }

  @GET
  @NoAuth
  public List<QueuedRequestId> getQueuedRequestIds() {
    return manager.getQueuedRequestIds();
  }

  @GET
  @NoAuth
  @Path("/history/{serviceId}")
  public List<BaragonResponse> getRecentRequestIds(@PathParam("serviceId") String serviceId) {
    return manager.getResponsesForService(serviceId);
  }

  @DELETE
  @Path("/{requestId}")
  public BaragonResponse cancelRequest(@PathParam("requestId") String requestId) {
    // prevent race conditions when transitioning from a cancel-able to not cancel-able state
    synchronized (BaragonRequestWorker.class) {
      manager.cancelRequest(requestId);
      return manager.getResponse(requestId).or(BaragonResponse.requestDoesNotExist(requestId));
    }
  }

  @PUT
  @Path("/upstreams/{serviceId}")
  public BaragonResponse addUpstream(@PathParam("serviceId") String serviceId, @QueryParam("upstream") String upstream) {
    UpstreamInfo upstreamInfo = UpstreamInfo.fromString(upstream);
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (!maybeService.isPresent()) {
      throw new WebApplicationException(String.format("Service %s not found", serviceId), 400);
    }
    return enqueueRequest(new BaragonRequestBuilder().setLoadBalancerRequestId(UUID.randomUUID().toString())
        .setLoadBalancerService(maybeService.get())
        .setAddUpstreams(Collections.singletonList(upstreamInfo))
        .setRemoveUpstreams(Collections.emptyList())
        .setReplaceUpstreams(Collections.emptyList())
        .setAction(Optional.absent())
        .setNoValidate(true)
        .setNoReload(false)
        .setUpstreamUpdateOnly(true)
        .build());
  }

  @POST
  @Path("/upstreams/{serviceId}")
  public BaragonResponse setUpstreams(@PathParam("serviceId") String serviceId, List<UpstreamInfo> upstreams) {
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (!maybeService.isPresent()) {
      throw new WebApplicationException(String.format("Service %s not found", serviceId), 400);
    }
    return enqueueRequest(new BaragonRequestBuilder().setLoadBalancerRequestId(UUID.randomUUID().toString())
        .setLoadBalancerService(maybeService.get())
        .setAddUpstreams(Collections.emptyList())
        .setRemoveUpstreams(Collections.emptyList())
        .setReplaceUpstreams(upstreams)
        .setAction(Optional.absent())
        .setNoValidate(true)
        .setNoReload(false)
        .setUpstreamUpdateOnly(true)
        .build());
  }

  @DELETE
  @Path("/upstreams/{serviceId}")
  public BaragonResponse removeUpstream(@PathParam("serviceId") String serviceId, @QueryParam("upstream") String upstream) {
    UpstreamInfo upstreamInfo = UpstreamInfo.fromString(upstream);
    Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
    if (!maybeService.isPresent()) {
      throw new WebApplicationException(String.format("Service %s not found", serviceId), 400);
    }
    return removeUpstream(maybeService.get(), Collections.singletonList(upstreamInfo));
  }

  private BaragonResponse removeUpstream(BaragonService service, List<UpstreamInfo> upstreamInfos) {
    return enqueueRequest(new BaragonRequestBuilder().setLoadBalancerRequestId(UUID.randomUUID().toString())
        .setLoadBalancerService(service)
        .setAddUpstreams(Collections.emptyList())
        .setRemoveUpstreams(upstreamInfos)
        .setReplaceUpstreams(Collections.emptyList())
        .setAction(Optional.absent())
        .setNoValidate(true)
        .setNoReload(false)
        .setUpstreamUpdateOnly(true)
        .build());
  }

  @DELETE
  @Path("/upstream")
  public Set<BaragonResponse> deleteOccurencesOfUpstream(@QueryParam("host") String host) {
    if (host == null) {
      throw new WebApplicationException("host must not be null", 400);
    }
    Set<BaragonResponse> results = new HashSet<>();
    for (BaragonServiceState serviceState : stateDatastore.getGlobalState()) {
      List<UpstreamInfo> matching = serviceState.getUpstreams().stream()
          .filter((u) -> u.getUpstream().split(":")[0].equals(host))
          .collect(Collectors.toList());
      if (!matching.isEmpty()) {
        results.add(removeUpstream(serviceState.getService(), matching));
      }
    }
    return results;
  }
}

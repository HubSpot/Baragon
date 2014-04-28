package com.hubspot.baragon.agent.resources;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.agent.models.ServiceContext;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.RequestState;
import com.hubspot.baragon.models.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final Lock agentLock;
  
  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore,
                         BaragonRequestDatastore requestDatastore,
                         FilesystemConfigHelper configHelper,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock) {
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.requestDatastore = requestDatastore;
    this.agentLock = agentLock;
  }

  @POST
  public Optional<ServiceContext> apply(@PathParam("requestId") String requestId) throws InterruptedException {
    // Acquire agent lock
    if (!agentLock.tryLock(5, TimeUnit.SECONDS)) {
      throw new WebApplicationException(Response.Status.CONFLICT);
    }

    try {
      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

      if (!maybeRequest.isPresent()) {
        return Optional.absent();
      }

      final BaragonRequest request = maybeRequest.get();

      // Apply request
      final Set<String> upstreams = Sets.newHashSet(stateDatastore.getUpstreams(request.getLoadBalancerService().getServiceId()));

      upstreams.removeAll(request.getRemoveUpstreams());
      upstreams.addAll(request.getAddUpstreams());

      final ServiceContext update = new ServiceContext(request.getLoadBalancerService(), upstreams, System.currentTimeMillis());

      configHelper.apply(update);

      return Optional.of(update);
    } finally {
      agentLock.unlock();
    }
  }

  @DELETE
  public Optional<ServiceContext> revert(@PathParam("requestId") String requestId) throws InterruptedException {
    if (!agentLock.tryLock(5, TimeUnit.SECONDS)) {
      throw new WebApplicationException(Response.Status.CONFLICT);
    }

    try {
      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

      if (!maybeRequest.isPresent()) {
        return Optional.absent();  // can't revert what's not there, idiot.
      }

      final Optional<BaragonResponse> maybeResponse = requestDatastore.getResponse(requestId);

      // can only revert when in WAITING state
      if (maybeResponse.isPresent() && maybeResponse.get().getLoadBalancerState() != RequestState.WAITING) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(String.format("Can't revert request in %s state", maybeResponse.get().getLoadBalancerState())).build());
      }

      final BaragonRequest request = maybeRequest.get();
      final Optional<Service> maybeService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

      if (!maybeService.isPresent()) {
        configHelper.remove(request.getLoadBalancerService().getServiceId());  // TODO: reload configs?
        return Optional.absent();
      }

      final ServiceContext context = new ServiceContext(maybeService.get(), stateDatastore.getUpstreams(maybeService.get().getServiceId()), System.currentTimeMillis());

      configHelper.apply(context);

      return Optional.of(context);
    } finally {
      agentLock.unlock();
    }
  }
}

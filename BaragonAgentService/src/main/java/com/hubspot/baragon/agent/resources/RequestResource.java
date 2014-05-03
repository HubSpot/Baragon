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
import com.hubspot.baragon.models.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final Lock agentLock;
  private final AtomicReference<String> mostRecentRequestId;
  private final long agentLockTimeoutMs;
  
  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore,
                         BaragonRequestDatastore requestDatastore,
                         FilesystemConfigHelper configHelper,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock,
                         @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.requestDatastore = requestDatastore;
    this.agentLock = agentLock;
    this.mostRecentRequestId = mostRecentRequestId;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  private void acquireAgentLock() {
    // Acquire agent lock
    try {
      if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    } catch (InterruptedException e) {
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  public Optional<ServiceContext> apply(@PathParam("requestId") String requestId) {
    acquireAgentLock();

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

      try {
        configHelper.apply(update, true);
      } catch (Exception e) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e).build());
      }

      mostRecentRequestId.set(requestId);

      return Optional.of(update);
    } finally {
      agentLock.unlock();
    }
  }

  @DELETE
  public Optional<ServiceContext> revert(@PathParam("requestId") String requestId) {
    acquireAgentLock();

    try {
      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

      if (!maybeRequest.isPresent()) {
        return Optional.absent();  // can't revert what's not there, idiot.
      }

      final BaragonRequest request = maybeRequest.get();
      final Optional<Service> maybeService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

      if (!maybeService.isPresent()) {
        try {
          configHelper.remove(request.getLoadBalancerService().getServiceId(), true);
        } catch (Exception e) {
          throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e).build());
        }
        return Optional.absent();
      }

      final ServiceContext context = new ServiceContext(maybeService.get(), stateDatastore.getUpstreams(maybeService.get().getServiceId()), System.currentTimeMillis());

      try {
        configHelper.apply(context, false);
      } catch (Exception e) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(e).build());
      }

      return Optional.of(context);
    } finally {
      agentLock.unlock();
    }
  }
}

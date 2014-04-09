package com.hubspot.baragon.agent.resources;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.BaragonRequest;

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
  private final LbConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final Lock agentLock;
  
  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore, BaragonRequestDatastore requestDatastore,
                         LbConfigHelper configHelper, @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock) {
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.requestDatastore = requestDatastore;
    this.agentLock = agentLock;
  }

  @POST
  public Optional<ServiceContext> apply(@PathParam("requestId") String requestId) {
    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      return Optional.absent();
    }

    final BaragonRequest request = maybeRequest.get();

    try {
      if (agentLock.tryLock(5, TimeUnit.SECONDS)) {
        try {

          final Set<String> upstreams = Sets.newHashSet(stateDatastore.getUpstreams(request.getService().getId()));

          upstreams.removeAll(request.getRemove());
          upstreams.addAll(request.getAdd());

          final ServiceContext update = new ServiceContext(request.getService(), upstreams, System.currentTimeMillis());

          configHelper.apply(update);

          return Optional.of(update);
        } finally {
          agentLock.unlock();
        }
      } else {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);  // TODO: is this what i should do?
    }
  }

  @DELETE
  public Optional<ServiceContext> revert(@PathParam("requestId") String requestId) {
    try {
      if (agentLock.tryLock(5, TimeUnit.SECONDS)) {
        try {
          final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

          if (!maybeRequest.isPresent()) {
            return Optional.absent();
          }

          final BaragonRequest request = maybeRequest.get();

          final Optional<Service> maybeServiceInfo = stateDatastore.getService(request.getService().getId());

          if (!maybeServiceInfo.isPresent()) {
            return Optional.absent();
          }

          final Service service = maybeServiceInfo.get();

          final ServiceContext snapshot = new ServiceContext(service, stateDatastore.getUpstreams(service.getId()), System.currentTimeMillis());

          configHelper.apply(snapshot);

          return Optional.of(snapshot);
        } finally {
          agentLock.unlock();
        }
      } else {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
}

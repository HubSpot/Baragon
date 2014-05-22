package com.hubspot.baragon.agent.resources;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.TestingConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.agent.models.ServiceContext;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Log LOG = LogFactory.getLog(RequestResource.class);

  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final Lock agentLock;
  private final AtomicReference<String> mostRecentRequestId;
  private final long agentLockTimeoutMs;
  private final Optional<TestingConfiguration> maybeTestingConfiguration;
  private final Random random;
  
  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore,
                         BaragonRequestDatastore requestDatastore,
                         FilesystemConfigHelper configHelper,
                         Optional<TestingConfiguration> maybeTestingConfiguration,
                         Random random,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock,
                         @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                         @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.maybeTestingConfiguration = maybeTestingConfiguration;
    this.requestDatastore = requestDatastore;
    this.agentLock = agentLock;
    this.mostRecentRequestId = mostRecentRequestId;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
    this.random = random;
  }

  @POST
  public Response apply(@PathParam("requestId") String requestId) throws InterruptedException {
    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      return Response.status(Response.Status.CONFLICT).build();
    }

    try {
      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

      if (!maybeRequest.isPresent()) {
        return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
      }

      final BaragonRequest request = maybeRequest.get();

      LOG.info(String.format("Received request to apply %s", request));

      // Apply request
      final Set<String> upstreams = Sets.newHashSet(stateDatastore.getUpstreams(request.getLoadBalancerService().getServiceId()));

      upstreams.removeAll(request.getRemoveUpstreams());
      upstreams.addAll(request.getAddUpstreams());

      final ServiceContext update = new ServiceContext(request.getLoadBalancerService(), upstreams, System.currentTimeMillis());

      if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled() && maybeTestingConfiguration.get().getApplyDelayMs() > 0) {
        try {
          Thread.sleep(maybeTestingConfiguration.get().getApplyDelayMs());
        } catch (InterruptedException e) {
          // boo
        }
      }

      if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled()) {
        if (random.nextFloat() <= maybeTestingConfiguration.get().getApplyFailRate()) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Random testing failure").build();
        }
      }

      try {
        configHelper.apply(update, true);
      } catch (Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
      }

      mostRecentRequestId.set(requestId);

      return Response.ok().build();
    } finally {
      agentLock.unlock();
    }
  }

  @DELETE
  public Response revert(@PathParam("requestId") String requestId) throws InterruptedException {
    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      return Response.status(Response.Status.CONFLICT).build();
    }

    try {
      final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

      if (!maybeRequest.isPresent()) {
        return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
      }

      final BaragonRequest request = maybeRequest.get();
      final Optional<BaragonService> maybeService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

      if (!maybeService.isPresent()) {
        try {
          configHelper.remove(request.getLoadBalancerService().getServiceId(), true);
        } catch (Exception e) {
          return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        return Response.ok().build();
      }

      final ServiceContext context = new ServiceContext(maybeService.get(), stateDatastore.getUpstreams(maybeService.get().getServiceId()), System.currentTimeMillis());

      if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled() && maybeTestingConfiguration.get().getRevertDelayMs() > 0) {
        try {
          Thread.sleep(maybeTestingConfiguration.get().getRevertDelayMs());
        } catch (InterruptedException e) {
          // boo
        }
      }

      if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled()) {
        if (random.nextFloat() <= maybeTestingConfiguration.get().getRevertFailRate()) {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Random testing failure").build();
        }
      }

      try {
        configHelper.apply(context, false);
      } catch (Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
      }

      return Response.ok().build();
    } finally {
      agentLock.unlock();
    }
  }
}

package com.hubspot.baragon.agent.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.config.TestingConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.exceptions.MissingTemplateException;

@Singleton
public class AgentRequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(AgentRequestManager.class);
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final AtomicReference<String> mostRecentRequestId;
  private final Optional<TestingConfiguration> maybeTestingConfiguration;
  private final Random random;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Lock agentLock;
  private final long agentLockTimeoutMs;

  @Inject
  public AgentRequestManager(BaragonStateDatastore stateDatastore,
                        BaragonRequestDatastore requestDatastore,
                        FilesystemConfigHelper configHelper,
                        Optional<TestingConfiguration> maybeTestingConfiguration,
                        LoadBalancerConfiguration loadBalancerConfiguration,
                        Random random,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK) Lock agentLock,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.stateDatastore = stateDatastore;
    this.configHelper = configHelper;
    this.maybeTestingConfiguration = maybeTestingConfiguration;
    this.requestDatastore = requestDatastore;
    this.mostRecentRequestId = mostRecentRequestId;
    this.random = random;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public Response processRequest(String requestId, Optional<RequestAction> maybeAction) throws InterruptedException {
    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      return Response.status(Response.Status.CONFLICT).build();
    }
    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }
    final BaragonRequest request = maybeRequest.get();
    RequestAction action = maybeAction.or(request.getAction().or(RequestAction.UPDATE));
    Optional<BaragonService> maybeOldService = getOldService(request);

    try {
      LOG.info(String.format("Received request to %s with id %s", action, requestId));
      switch (action) {
        case DELETE:
          return delete(request, maybeOldService);
        case RELOAD:
          return reload();
        case REVERT:
          return revert(request, maybeOldService);
        default:
          return apply(request, maybeOldService);
      }
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while %sING for reqeust %s", action, requestId), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(String.format("Caught exception while %sING for reqeust %s: %s", action, requestId, e.getMessage())).build();
    } finally {
      LOG.info(String.format("Done processing %s request: %s", action, requestId));
      agentLock.unlock();
    }
  }

  private Response reload() throws Exception {
    configHelper.checkAndReload();
    return Response.ok().build();
  }

  private Response delete(BaragonRequest request, Optional<BaragonService> maybeOldService) throws Exception {
    try {
      configHelper.delete(request.getLoadBalancerService(), maybeOldService);
      return Response.ok().build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

  private Response apply(BaragonRequest request, Optional<BaragonService> maybeOldService) throws Exception {
    final ServiceContext update = getApplyContext(request);
    triggerTesting();
    try {
      configHelper.apply(update, maybeOldService, true);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    mostRecentRequestId.set(request.getLoadBalancerRequestId());
    return Response.ok().build();
  }

  private Response revert(BaragonRequest request, Optional<BaragonService> maybeOldService) throws Exception {
    final ServiceContext update;
    if (movedOffLoadBalancer(maybeOldService)) {
      update = new ServiceContext(request.getLoadBalancerService(), Collections.<UpstreamInfo>emptyList(), System.currentTimeMillis(), false);
    } else {
      update = new ServiceContext(maybeOldService.get(), stateDatastore.getUpstreamsMap(maybeOldService.get().getServiceId()).values(), System.currentTimeMillis(), true);
    }

    triggerTesting();

    LOG.info(String.format("Reverting to %s", update));
    try {
      configHelper.apply(update, Optional.<BaragonService>absent(), false);
    } catch (MissingTemplateException e) {
      if (serviceDidNotPreviouslyExist(maybeOldService)) {
        return Response.ok().build();
      } else {
        throw e;
      }
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    return Response.ok().build();
  }

  private ServiceContext getApplyContext(BaragonRequest request) throws Exception {
    if (movedOffLoadBalancer(request)) {
      return new ServiceContext(request.getLoadBalancerService(), Collections.<UpstreamInfo>emptyList(), System.currentTimeMillis(), false);
    } else {
      final Map<String, UpstreamInfo> upstreamsMap = new HashMap<>();
      upstreamsMap.putAll(stateDatastore.getUpstreamsMap(request.getLoadBalancerService().getServiceId()));

      for (UpstreamInfo removeUpstreamInfo : request.getRemoveUpstreams()) {
        upstreamsMap.remove(removeUpstreamInfo.getUpstream());
      }

      for (UpstreamInfo addUpstreamInfo : request.getAddUpstreams()) {
        upstreamsMap.put(addUpstreamInfo.getUpstream(), addUpstreamInfo);
      }

      return new ServiceContext(request.getLoadBalancerService(), upstreamsMap.values(), System.currentTimeMillis(), true);
    }
  }

  private boolean movedOffLoadBalancer(Optional<BaragonService> maybeOldService) {
    return (!maybeOldService.isPresent() || !maybeOldService.get().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName()));
  }

  private boolean movedOffLoadBalancer(BaragonRequest request) {
    return (!request.getLoadBalancerService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName()));
  }

  private boolean serviceDidNotPreviouslyExist(Optional<BaragonService> maybeOldService) {
    return (!maybeOldService.isPresent() || !maybeOldService.get().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName()));
  }

  private Optional<BaragonService> getOldService(BaragonRequest request) {
    Optional<BaragonService> service = Optional.absent();
    if (request.getReplaceServiceId().isPresent()) {
      service = stateDatastore.getService(request.getReplaceServiceId().get());
    }
    if (service.isPresent()) {
      return service;
    } else {
      return stateDatastore.getService(request.getLoadBalancerService().getServiceId());
    }
  }

  private void triggerTesting() throws Exception {
    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled() && maybeTestingConfiguration.get().getApplyDelayMs() > 0) {
      Thread.sleep(maybeTestingConfiguration.get().getApplyDelayMs());
    }

    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled()) {
      if (random.nextFloat() <= maybeTestingConfiguration.get().getApplyFailRate()) {
        throw new Exception("Random testing failure");
      }
    }
  }
}
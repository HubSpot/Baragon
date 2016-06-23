package com.hubspot.baragon.agent.managers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.config.TestingConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.LockTimeoutException;
import com.hubspot.baragon.exceptions.MissingTemplateException;
import com.hubspot.baragon.models.AgentBatchResponseItem;
import com.hubspot.baragon.models.BaragonAgentState;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBatchItem;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;

@Singleton
public class AgentRequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(AgentRequestManager.class);
  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final AtomicReference<String> mostRecentRequestId;
  private final Optional<TestingConfiguration> maybeTestingConfiguration;
  private final Random random;
  private final AtomicReference<BaragonAgentState> agentState;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final long agentLockTimeoutMs;

  @Inject
  public AgentRequestManager(BaragonStateDatastore stateDatastore,
                        BaragonRequestDatastore requestDatastore,
                        FilesystemConfigHelper configHelper,
                        Optional<TestingConfiguration> maybeTestingConfiguration,
                        LoadBalancerConfiguration loadBalancerConfiguration,
                        Random random,
                        AtomicReference<BaragonAgentState> agentState,
                        @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                        @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.stateDatastore = stateDatastore;
    this.configHelper = configHelper;
    this.maybeTestingConfiguration = maybeTestingConfiguration;
    this.requestDatastore = requestDatastore;
    this.mostRecentRequestId = mostRecentRequestId;
    this.random = random;
    this.agentState = agentState;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public List<AgentBatchResponseItem> processRequests(List<BaragonRequestBatchItem> batch) throws InterruptedException {
    List<AgentBatchResponseItem> responses = Lists.newArrayList();
    int i = 0;
    for (BaragonRequestBatchItem item : batch) {
      boolean isLast = i == batch.size() - 1;
      responses.add(getResponseItem(processRequest(item.getRequestId(), actionForBatchItem(item), !isLast), item));
      i++;
    }
    return responses;
  }

  private AgentBatchResponseItem getResponseItem(Response httpResponse, BaragonRequestBatchItem item) {
    Optional<String> maybeMessage = httpResponse.getEntity() != null ? Optional.of(httpResponse.getEntity().toString()) : Optional.<String>absent();
    return new AgentBatchResponseItem(item.getRequestId(), httpResponse.getStatus(), maybeMessage, item.getRequestType());
  }

  private Optional<RequestAction> actionForBatchItem(BaragonRequestBatchItem item) {
    if (item.getRequestAction().isPresent()) {
      return item.getRequestAction();
    } else {
      switch (item.getRequestType()) {
        case REVERT:
        case CANCEL:
          return Optional.of(RequestAction.REVERT);
        case APPLY:
        default:
          return Optional.of(RequestAction.UPDATE);
      }
    }
  }

  public Response processRequest(String requestId, Optional<RequestAction> maybeAction, boolean delayReload) throws InterruptedException {
    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }
    final BaragonRequest request = maybeRequest.get();
    RequestAction action = maybeAction.or(request.getAction().or(RequestAction.UPDATE));
    Optional<BaragonService> maybeOldService = getOldService(request);

    try {
      agentState.set(BaragonAgentState.APPLYING);
      LOG.info(String.format("Received request to %s with id %s", action, requestId));
      switch (action) {
        case DELETE:
          return delete(request, maybeOldService, delayReload);
        case RELOAD:
          return reload(request, delayReload);
        case REVERT:
          return revert(request, maybeOldService, delayReload);
        default:
          return apply(request, maybeOldService, delayReload);
      }
    } catch (LockTimeoutException e) {
      LOG.warn(String.format("Couldn't acquire agent lock for %s in %s ms", requestId, agentLockTimeoutMs), e);
      return Response.status(Response.Status.CONFLICT).entity(String.format("Couldn't acquire agent lock for %s in %s ms. Lock Info: %s", requestId, agentLockTimeoutMs, e.getLockInfo())).build();
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while %sING for request %s", action, requestId), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(String.format("Caught exception while %sING for request %s: %s", action, requestId, e.getMessage())).build();
    } finally {
      LOG.info(String.format("Done processing %s request: %s", action, requestId));
      agentState.set(BaragonAgentState.ACCEPTING);
    }
  }

  private Response reload(BaragonRequest request, boolean delayReload) throws Exception {
    if (!delayReload) {
      configHelper.checkAndReload();
    }
    mostRecentRequestId.set(request.getLoadBalancerRequestId());
    return Response.ok().build();
  }

  private Response delete(BaragonRequest request, Optional<BaragonService> maybeOldService, boolean delayReload) throws Exception {
    try {
      configHelper.delete(request.getLoadBalancerService(), maybeOldService, request.isNoReload(), request.isNoValidate(), delayReload);
      mostRecentRequestId.set(request.getLoadBalancerRequestId());
      return Response.ok().build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
  }

  private Response apply(BaragonRequest request, Optional<BaragonService> maybeOldService, boolean delayReload) throws Exception {
    final ServiceContext update = getApplyContext(request);
    triggerTesting();
    try {
      configHelper.apply(update, maybeOldService, true, request.isNoReload(), request.isNoValidate(), delayReload);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    mostRecentRequestId.set(request.getLoadBalancerRequestId());
    return Response.ok().build();
  }

  private Response revert(BaragonRequest request, Optional<BaragonService> maybeOldService, boolean delayReload) throws Exception {
    final ServiceContext update;
    if (movedOffLoadBalancer(maybeOldService)) {
      update = new ServiceContext(request.getLoadBalancerService(), Collections.<UpstreamInfo>emptyList(), System.currentTimeMillis(), false);
    } else {
      update = new ServiceContext(maybeOldService.get(), stateDatastore.getUpstreams(maybeOldService.get().getServiceId()), System.currentTimeMillis(), true);
    }

    triggerTesting();

    LOG.info(String.format("Reverting to %s", update));
    try {
      configHelper.apply(update, Optional.<BaragonService>absent(), false, request.isNoReload(), request.isNoValidate(), delayReload);
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
    } else if (!request.getReplaceUpstreams().isEmpty()) {
      return new ServiceContext(request.getLoadBalancerService(), request.getReplaceUpstreams(), System.currentTimeMillis(), true);
    } else {
      List<UpstreamInfo> upstreams = new ArrayList<>();
      upstreams.addAll(request.getAddUpstreams());
      for (UpstreamInfo existingUpstream : stateDatastore.getUpstreams(request.getLoadBalancerService().getServiceId())) {
        boolean present = false;
        boolean toRemove = false;
        for (UpstreamInfo currentUpstream : upstreams) {
          if (UpstreamInfo.upstreamAndGroupMatches(currentUpstream, existingUpstream)) {
            present = true;
            break;
          }
        }
        for (UpstreamInfo upstreamToRemove : request.getRemoveUpstreams()) {
          if (UpstreamInfo.upstreamAndGroupMatches(upstreamToRemove, existingUpstream)) {
            toRemove = true;
            break;
          }
        }
        if (!present && !toRemove) {
          upstreams.add(existingUpstream);
        }
      }

      return new ServiceContext(request.getLoadBalancerService(), upstreams, System.currentTimeMillis(), true);
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

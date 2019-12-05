package com.hubspot.baragon.agent.healthcheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BasicServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;

import de.danielbechler.diff.ObjectDifferBuilder;

@Singleton
public class InternalStateChecker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(InternalStateChecker.class);

  private final BaragonStateDatastore stateDatastore;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final Map<String, BasicServiceContext> internalStateCache;
  private final Set<String> stateErrors;

  @Inject
  public InternalStateChecker(BaragonStateDatastore stateDatastore,
                              LoadBalancerConfiguration loadBalancerConfiguration,
                              @Named(BaragonAgentServiceModule.INTERNAL_STATE_CACHE) Map<String, BasicServiceContext> internalStateCache,
                              @Named(BaragonAgentServiceModule.LOCAL_STATE_ERROR_MESSAGE) Set<String> stateErrors) {
    this.stateDatastore = stateDatastore;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.internalStateCache = internalStateCache;
    this.stateErrors = stateErrors;
  }

  @Override
  public void run() {
    Set<String> invalidServiceMessages = new HashSet<>();
    long now = System.currentTimeMillis();
    internalStateCache.forEach((serviceId, context) -> {
      Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
      if (!maybeService.isPresent()) {
        invalidServiceMessages.add(String.format("%s no longer exists in state datastore, but exists in agent", serviceId));
        return;
      }
      if (!maybeService.get().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
        invalidServiceMessages.add(String.format("%s is no longer deployed to group %s", serviceId, loadBalancerConfiguration.getName()));
        return;
      }
      Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(serviceId);
      BasicServiceContext datastoreContext = new BasicServiceContext(maybeService.get(), existingUpstreams);
      if (!datastoreContext.equals(context) && now - context.getTimestamp() > TimeUnit.SECONDS.toMillis(60)) {
        invalidServiceMessages.add(getDiffMessage(context, datastoreContext));
      }
    });
    stateErrors.clear();
    if (!invalidServiceMessages.isEmpty()) {
      stateErrors.addAll(invalidServiceMessages);
    }
  }

  private String getDiffMessage(BasicServiceContext agentContext, BasicServiceContext datastoreContext) {
    try {
      return String.format("%s does not match state: %s", agentContext.getService().getServiceId(), ObjectDifferBuilder.buildDefault().compare(agentContext, datastoreContext).toString());
    } catch (Throwable t) {
      LOG.warn("Could not generate baragon service diff message", t);
      return String.format("Agent context for %s does not match baragon state (Agent: %s, Service: %s)", agentContext.getService().getServiceId(), agentContext, datastoreContext);
    }
  }
}

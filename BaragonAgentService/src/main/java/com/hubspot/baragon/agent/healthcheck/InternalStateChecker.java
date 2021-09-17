package com.hubspot.baragon.agent.healthcheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.BootstrapFileChecker;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.LockTimeoutException;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.BasicServiceContext;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InternalStateChecker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(InternalStateChecker.class);

  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final FilesystemConfigHelper configHelper;
  private final Map<String, BasicServiceContext> internalStateCache;
  private final Set<String> stateErrors;
  private final ObjectMapper objectMapper;
  private final ReentrantLock agentLock;

  @Inject
  public InternalStateChecker(
    BaragonStateDatastore stateDatastore,
    BaragonRequestDatastore requestDatastore,
    LoadBalancerConfiguration loadBalancerConfiguration,
    ObjectMapper objectMapper,
    FilesystemConfigHelper configHelper,
    @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock,
    @Named(
      BaragonAgentServiceModule.INTERNAL_STATE_CACHE
    ) Map<String, BasicServiceContext> internalStateCache,
    @Named(BaragonAgentServiceModule.LOCAL_STATE_ERROR_MESSAGE) Set<String> stateErrors
  ) {
    this.stateDatastore = stateDatastore;
    this.requestDatastore = requestDatastore;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.objectMapper = objectMapper;
    this.agentLock = agentLock;
    this.internalStateCache = internalStateCache;
    this.stateErrors = stateErrors;
  }

  @Override
  public void run() {
    Set<String> invalidServiceMessages = new HashSet<>();
    long now = System.currentTimeMillis();
    new HashMap<>(internalStateCache)
    .forEach(
        (serviceId, context) -> {
          Optional<BaragonService> maybeService = stateDatastore.getService(serviceId);
          if (!maybeService.isPresent()) {
            invalidServiceMessages.add(
              String.format(
                "%s no longer exists in state datastore, but exists in agent",
                serviceId
              )
            );
            return;
          }
          if (
            !maybeService
              .get()
              .getLoadBalancerGroups()
              .contains(loadBalancerConfiguration.getName())
          ) {
            invalidServiceMessages.add(
              String.format(
                "%s is no longer deployed to group %s",
                serviceId,
                loadBalancerConfiguration.getName()
              )
            );
            return;
          }
          Collection<UpstreamInfo> existingUpstreams = stateDatastore.getUpstreams(
            serviceId
          );
          BasicServiceContext datastoreContext = new BasicServiceContext(
            maybeService.get(),
            existingUpstreams
          );
          if (
            !datastoreContext.equals(context) &&
            now - context.getTimestamp() > TimeUnit.MINUTES.toMillis(2)
          ) {
            if (
              requestDatastore
                .getQueuedRequestIds()
                .stream()
                .noneMatch(q -> q.getServiceId().equals(serviceId))
            ) {
              Optional<BaragonService> maybeUpdatedService = stateDatastore.getService(
                serviceId
              );
              if (!maybeUpdatedService.isPresent()) {
                LOG.warn(
                  "Service data no longer present, skipping auto-fix for {}",
                  serviceId
                );
                invalidServiceMessages.add(getDiffMessage(context, datastoreContext));
                return;
              }
              Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>> maybeCheck = new BootstrapFileChecker(
                configHelper,
                new BaragonServiceState(
                  maybeUpdatedService.get(),
                  stateDatastore.getUpstreams(serviceId)
                ),
                now
              )
              .call();
              if (maybeCheck.isPresent()) {
                try {
                  if (!agentLock.tryLock(10, TimeUnit.MILLISECONDS)) {
                    LockTimeoutException lte = new LockTimeoutException(
                      "Timed out waiting to acquire lock",
                      agentLock
                    );
                    LOG.warn(
                      "Failed to acquire lock for service config apply ({})",
                      serviceId,
                      lte
                    );
                    throw lte;
                  }
                  try {
                    configHelper.bootstrapApply(
                      maybeCheck.get().getKey(),
                      maybeCheck.get().getValue()
                    );
                    BasicServiceContext newContext = new BasicServiceContext(
                      maybeCheck.get().getKey().getService(),
                      maybeCheck.get().getKey().getUpstreams()
                    );
                    internalStateCache.put(serviceId, newContext);
                  } finally {
                    agentLock.unlock();
                  }
                } catch (Exception e) {
                  invalidServiceMessages.add(getDiffMessage(context, datastoreContext));
                  LOG.error("Failed to auto-fix configs for {}", serviceId, e);
                }
              }
            }
          }
        }
      );
    stateErrors.clear();
    if (!invalidServiceMessages.isEmpty()) {
      stateErrors.addAll(invalidServiceMessages);
    }
  }

  private String getDiffMessage(
    BasicServiceContext agentContext,
    BasicServiceContext datastoreContext
  ) {
    try {
      JsonNode agent = objectMapper.valueToTree(agentContext);
      JsonNode datastore = objectMapper.valueToTree(datastoreContext);
      JsonNode diff = JsonDiff.asJson(agent, datastore);
      return String.format(
        "%s does not match state: %s",
        agentContext.getService().getServiceId(),
        diff.toString()
      );
    } catch (Throwable t) {
      LOG.warn("Could not generate baragon service diff message", t);
      return String.format(
        "Agent context for %s does not match baragon state (Agent: %s, Service: %s)",
        agentContext.getService().getServiceId(),
        agentContext,
        datastoreContext
      );
    }
  }
}

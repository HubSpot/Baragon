package com.hubspot.baragon.service.worker;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.managers.AgentManager;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.service.BaragonServiceModule;

public class BaragonRequestWorker implements Runnable {
  private static final Log LOG = LogFactory.getLog(BaragonRequestWorker.class);

  private final AgentManager agentManager;
  private final RequestManager requestManager;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonRequestWorker(AgentManager agentManager,
                              RequestManager requestManager,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.agentManager = agentManager;
    this.requestManager = requestManager;
    this.workerLastStartAt = workerLastStartAt;
  }

  public void handleRequest(QueuedRequestId queuedRequestId) {
    final String requestId = queuedRequestId.getRequestId();

    final Optional<InternalRequestStates> maybeStatus = requestManager.getRequestState(requestId);

    if (!maybeStatus.isPresent()) {
      LOG.warn(String.format("%s does not have a request status!", requestId));
      return;
    }

    final Optional<BaragonRequest> maybeRequest = requestManager.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      LOG.warn(String.format("%s does not have a request object!", requestId));
      return;
    }

    final Optional<InternalRequestStates> maybeNewStatus = maybeStatus.get().handle(maybeRequest.get(), agentManager, requestManager);

    if (maybeNewStatus.isPresent()) {
      LOG.info(String.format("%s: %s --> %s", requestId, maybeStatus.get(), maybeNewStatus.get()));
      requestManager.setRequestState(requestId, maybeNewStatus.get());
    }

    if (maybeNewStatus.or(maybeStatus.get()).isRemovable()) {
      requestManager.removeQueuedRequest(queuedRequestId);
    }
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());

    try {

      final List<QueuedRequestId> queuedRequestIds = requestManager.getQueuedRequestIds();

      if (!queuedRequestIds.isEmpty()) {
        final Set<String> handledServices = Sets.newHashSet();  // only handle one request per service at a time

        for (QueuedRequestId queuedRequestId : queuedRequestIds) {
          if (!handledServices.contains(queuedRequestId.getServiceId())) {
            synchronized (BaragonRequestWorker.class) {
              handleRequest(queuedRequestId);
            }
            handledServices.add(queuedRequestId.getServiceId());
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Caught exception", e);
    }
  }
}

package com.hubspot.baragon.service.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonResponseHistoryDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestKey;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestPurgingWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(RequestPurgingWorker.class);

  private final BaragonRequestDatastore requestDatastore;
  private final BaragonConfiguration configuration;
  private final BaragonAgentResponseDatastore agentResponseDatastore;
  private final BaragonResponseHistoryDatastore responseHistoryDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonExceptionNotifier exceptionNotifier;

  @Inject
  public RequestPurgingWorker(BaragonRequestDatastore requestDatastore,
                              BaragonConfiguration configuration,
                              BaragonAgentResponseDatastore agentResponseDatastore,
                              BaragonResponseHistoryDatastore responseHistoryDatastore,
                              BaragonStateDatastore stateDatastore,
                              BaragonExceptionNotifier exceptionNotifier) {
    this.requestDatastore = requestDatastore;
    this.configuration = configuration;
    this.agentResponseDatastore = agentResponseDatastore;
    this.responseHistoryDatastore = responseHistoryDatastore;
    this.stateDatastore = stateDatastore;
    this.exceptionNotifier = exceptionNotifier;
  }

  private enum PurgeAction {
    PURGE, SAVE, NONE
  }

  @Override
  public void run() {
    try {
      long referenceTime = System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(configuration.getHistoryConfiguration().getPurgeOldRequestsAfterDays()));
      cleanUpActiveRequests(referenceTime);
      if (configuration.getHistoryConfiguration().isPurgeOldRequests() && !Thread.interrupted()) {
        purgeHistoricalRequests(referenceTime);
        trimNumRequestsPerService();
      }
    } catch (Exception e) {
      LOG.error("Caught exception during old request purging", e);
      exceptionNotifier.notify(e, Collections.<String, String>emptyMap());
    }
  }

  public void cleanUpActiveRequests(long referenceTime) {
    List<String> allMaybeActiveRequestIds = requestDatastore.getAllRequestIds();
    for (String requestId : allMaybeActiveRequestIds) {
      try {
        Optional<InternalRequestStates> maybeState = requestDatastore.getRequestState(requestId);
        switch (getPurgeActionForMaybeActiveRequest(requestId, referenceTime, maybeState)) {
          case PURGE:
            requestDatastore.deleteRequest(requestId);
            break;
          case SAVE:
            Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
            if (maybeRequest.isPresent()) {
              BaragonResponse response = new BaragonResponse(maybeRequest.get().getLoadBalancerRequestId(), InternalStatesMap.getRequestState(maybeState.get()), requestDatastore.getRequestMessage(maybeRequest.get().getLoadBalancerRequestId()), Optional.of(agentResponseDatastore.getLastResponses(maybeRequest.get().getLoadBalancerRequestId())), maybeRequest);
              responseHistoryDatastore.addResponse(maybeRequest.get().getLoadBalancerService().getServiceId(), maybeRequest.get().getLoadBalancerRequestId(), response);
              requestDatastore.deleteRequest(requestId);
            } else {
              LOG.warn("Could not get request data to save history for request {}", requestId);
            }
            break;
          case NONE:
          default:
            break;
        }
      } catch (Exception e) {
        LOG.error("Caught exception trying to clean up request {}", requestId, e);
        exceptionNotifier.notify(e, ImmutableMap.of("requestId", requestId));
      }
      if (Thread.interrupted()) {
        LOG.warn("Purger was interrupted, stopping purge");
        break;
      }
    }
  }

  private PurgeAction getPurgeActionForMaybeActiveRequest(String requestId, long referenceTime, Optional<InternalRequestStates> maybeState) {
    Optional<Long> maybeUpdatedAt = requestDatastore.getRequestUpdatedAt(requestId);
    if (!maybeState.isPresent() || InternalStatesMap.isRemovable(maybeState.get())) {
      if (configuration.getHistoryConfiguration().isPurgeOldRequests()) {
        if (shouldPurge(maybeUpdatedAt, referenceTime)) {
          LOG.trace("Updated at time: {} is earlier than reference time: {}, purging request {}", maybeUpdatedAt.get(), referenceTime, requestId);
          return PurgeAction.PURGE;
        } else {
          return PurgeAction.SAVE;
        }
      } else {
        return PurgeAction.SAVE;
      }
    } else {
      return PurgeAction.NONE;
    }
  }

  private void purgeHistoricalRequests(long referenceTime) {
    for (String serviceId : responseHistoryDatastore.getServiceIds()) {
      if (!serviceId.equals("requestIdMapping")) {
        List<String> requestIds = responseHistoryDatastore.getRequestIdsForService(serviceId);
        if (stateDatastore.serviceExists(serviceId)) {
          if (!requestIds.isEmpty()) {
            for (String requestId : requestIds) {
              Optional<Long> maybeUpdatedAt = responseHistoryDatastore.getRequestUpdatedAt(serviceId, requestId);
              if (shouldPurge(maybeUpdatedAt, referenceTime)) {
                LOG.trace("Updated at time: {} is earlier than reference time: {}, purging request {}", maybeUpdatedAt.get(), referenceTime, requestId);
                responseHistoryDatastore.deleteResponse(serviceId, requestId);
              }
              if (Thread.interrupted()) {
                LOG.warn("Purger was interrupted, stopping purge");
                break;
              }
            }
          }
        } else {
          responseHistoryDatastore.deleteResponses(serviceId);
        }
      }
      if (Thread.interrupted()) {
        LOG.warn("Purger was interrupted, stopping purge");
        break;
      }
    }
  }

  private boolean shouldPurge(Optional<Long> maybeUpdatedAt, long referenceTime) {
    return (maybeUpdatedAt.isPresent() && maybeUpdatedAt.get() < referenceTime) || (!maybeUpdatedAt.isPresent() && configuration.getHistoryConfiguration().isPurgeWhenDateNotFound());
  }

  private void trimNumRequestsPerService() {
    LOG.trace("Checking for services with too many requests");
    for (String serviceId : responseHistoryDatastore.getServiceIds()) {
      if (!serviceId.equals("requestIdMapping")) {
        try {
          List<String> requestIds = responseHistoryDatastore.getRequestIdsForService(serviceId);
          if (requestIds.size() > configuration.getHistoryConfiguration().getMaxRequestsPerService()) {
            removeOldestRequestIds(serviceId, requestIds);
          }
        } catch (Exception e) {
          LOG.error("Caught exception purging old requests for service {}", serviceId, e);
          exceptionNotifier.notify(e, ImmutableMap.of("serviceId", serviceId));
        }
      }
    }
  }

  private void removeOldestRequestIds(String serviceId, List<String> requestIds) {
    LOG.debug("Service {} has {} requests, over limit of {}, will remove oldest requests", serviceId, requestIds.size(), configuration.getHistoryConfiguration().getMaxRequestsPerService());
    List<BaragonRequestKey> requestKeyList = new ArrayList<>();
    for (String requestId : requestIds) {
      Optional<Long> maybeUpdatedAt = responseHistoryDatastore.getRequestUpdatedAt(serviceId, requestId);
      if (maybeUpdatedAt.isPresent()) {
        requestKeyList.add(new BaragonRequestKey(requestId, maybeUpdatedAt.get()));
      } else {
        if (configuration.getHistoryConfiguration().isPurgeWhenDateNotFound()) {
          responseHistoryDatastore.deleteResponse(serviceId, requestId);
        }
      }
    }
    Collections.sort(requestKeyList);
    for (BaragonRequestKey requestKey : requestKeyList.subList(configuration.getHistoryConfiguration().getMaxRequestsPerService(), requestKeyList.size())) {
      responseHistoryDatastore.deleteResponse(serviceId, requestKey.getRequestId());
    }
  }
}

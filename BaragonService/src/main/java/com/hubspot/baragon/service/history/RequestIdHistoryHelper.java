package com.hubspot.baragon.service.history;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestIdHistoryHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RequestIdHistoryHelper.class);

  private final BaragonRequestDatastore requestDatastore;
  private final HistoryManager historyManager;
  private final BaragonAgentResponseDatastore agentResponseDatastore;


  @Inject
  public RequestIdHistoryHelper(BaragonRequestDatastore requestDatastore,
                                HistoryManager historyManager,
                                BaragonAgentResponseDatastore agentResponseDatastore) {
    this.requestDatastore = requestDatastore;
    this.historyManager = historyManager;
    this.agentResponseDatastore = agentResponseDatastore;
  }

  public Optional<BaragonRequest> getRequestById(String requestId) {
    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
    if (maybeRequest.isPresent()) {
      return maybeRequest;
    } else {
      return historyManager.getRequestById(requestId);
    }
  }

  public Optional<BaragonResponse> getResponseById(String requestId) {
    Optional<BaragonResponse> maybeResponse = getResponseFromZk(requestId);
    if (maybeResponse.isPresent()) {
      return maybeResponse;
    } else {
      return historyManager.getRequestResponse(requestId);
    }
  }

  public List<String> getBlendedHistory(Integer limitStart, Integer limitCount) {
    return getBlendedHistory(Optional.<String>absent(), limitStart, limitCount);
  }

  public List<String> getBlendedHistory(Optional<String> serviceId, Integer limitStart, Integer limitCount) {
    final List<String> fromZk = getFromZk(serviceId);

    final int numFromZk = Math.max(0, Math.min(limitCount, fromZk.size() - limitStart));

    final Integer numFromHistory = limitCount - numFromZk;
    final Integer historyStart = Math.max(0, limitStart - fromZk.size());

    List<String> returned = Lists.newArrayListWithCapacity(limitCount);

    if (numFromZk > 0) {
      returned.addAll(fromZk.subList(limitStart, limitStart + numFromZk));
    }

    if (numFromHistory > 0) {
      returned.addAll(getFromHistory(serviceId, historyStart, numFromHistory));
    }

    return returned;
  }

  private Optional<BaragonResponse> getResponseFromZk(String requestId) {
    final Optional<InternalRequestStates> maybeStatus = requestDatastore.getRequestState(requestId);

    if (!maybeStatus.isPresent()) {
      return Optional.absent();
    }

    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
    if (!maybeRequest.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new BaragonResponse(requestId, InternalStatesMap.getRequestState(maybeStatus.get()), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId)), maybeRequest));
  }

  protected List<String> getFromZk(Optional<String> serviceId) {
    List<String> requestIds = requestDatastore.getAllRequestIds();
    List<String> toRemove = new ArrayList<>();
    if (serviceId.isPresent()) {
      for (String requestId  :requestIds) {
        Optional<BaragonRequest> request = requestDatastore.getRequest(requestId);
        if (request.isPresent() && !request.get().getLoadBalancerService().getServiceId().equals(serviceId.get())) {
          toRemove.add(requestId);
        }
      }
      requestIds.removeAll(toRemove);
    }
    return requestIds;
  }

  protected List<String> getFromHistory(Optional<String> serviceId, int historyStart, int numFromHistory) {
    if (serviceId.isPresent()) {
      return historyManager.getReuqestIdsForService(serviceId.get(), historyStart, numFromHistory);
    } else {
      return historyManager.getRequestIds(historyStart, numFromHistory);
    }
  }
}

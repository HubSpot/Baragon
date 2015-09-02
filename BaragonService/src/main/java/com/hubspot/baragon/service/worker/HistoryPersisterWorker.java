package com.hubspot.baragon.service.worker;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.service.history.HistoryManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.InternalStatesMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HistoryPersisterWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryPersisterWorker.class);

  private final BaragonRequestDatastore requestDatastore;
  private final BaragonAgentResponseDatastore agentResponseDatastore;
  private final HistoryManager historyManager;

  @Inject
  public HistoryPersisterWorker(BaragonRequestDatastore requestDatastore,
                                BaragonAgentResponseDatastore agentResponseDatastore,
                                HistoryManager historyManager) {
    this.requestDatastore = requestDatastore;
    this.agentResponseDatastore = agentResponseDatastore;
    this.historyManager = historyManager;
  }

  @Override
  public void run() {
    List<String> allRequestIds = requestDatastore.getAllRequestIds();
    for (String requestId : allRequestIds) {
      try {
        Optional<InternalRequestStates> maybeState = requestDatastore.getRequestState(requestId);
        Optional<BaragonResponse> maybePersistedResponse = historyManager.getRequestResponse(requestId);
        if (maybeState.isPresent() && InternalStatesMap.isRemovable(maybeState.get())) {
          if (!maybePersistedResponse.isPresent()) {
            final Optional<InternalRequestStates> maybeStatus = requestDatastore.getRequestState(requestId);
            final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);
            Optional<Date> maybeUpdatedAt = requestDatastore.getRequestUpdatedAt(requestId);

            Date updatedAt;
            if (maybeUpdatedAt.isPresent()) {
              updatedAt = maybeUpdatedAt.get();
            } else {
              updatedAt = new Date();
            }

            BaragonResponse response = new BaragonResponse(requestId, InternalStatesMap.getRequestState(maybeStatus.get()), requestDatastore.getRequestMessage(requestId), Optional.of(agentResponseDatastore.getLastResponses(requestId)), maybeRequest);
            historyManager.saveRequestHistory(response, updatedAt);
          }
          requestDatastore.deleteRequest(requestId);
        } else {
          LOG.trace(String.format("Request %s still in progress, will not persist to db",requestId));
        }
      } catch (Exception e) {
        LOG.warn(String.format("Could not persist history for request: %s", requestId), e);
      }
    }
  }
}

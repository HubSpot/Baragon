package com.hubspot.baragon.service.managers;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class StatusManager {
  private static final Logger LOG = LoggerFactory.getLogger(StatusManager.class);

  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong workerLastStart;
  private final AtomicLong elbWorkerLastStart;
  private final AtomicReference<ConnectionState> connectionState;
  private final BaragonWorkerDatastore workerDatastore;
  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Inject
  public StatusManager(BaragonRequestDatastore requestDatastore,
                       BaragonStateDatastore stateDatastore,
                       BaragonWorkerDatastore workerDatastore,
                       ObjectMapper objectMapper,
                       @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                       @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStart,
                       @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong elbWorkerLastStart,
                       @Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState,
                       @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT)AsyncHttpClient httpClient) {
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
    this.workerDatastore = workerDatastore;
    this.leaderLatch = leaderLatch;
    this.workerLastStart = workerLastStart;
    this.elbWorkerLastStart = elbWorkerLastStart;
    this.connectionState = connectionState;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public BaragonServiceStatus getServiceStatus() {
    final ConnectionState currentConnectionState = connectionState.get();
    final String connectionStateString = currentConnectionState == null ? "UNKNOWN" : currentConnectionState.name();
    final long workerLagMs = System.currentTimeMillis() - workerLastStart.get();
    final long elbWorkerLagMs = System.currentTimeMillis() - elbWorkerLastStart.get();
    if (connectionStateString.equals("CONNECTED") || connectionStateString.equals("RECONNECTED")) {
      final int globalStateNodeSize = stateDatastore.getGlobalStateSize();
      return new BaragonServiceStatus(leaderLatch.hasLeadership(), requestDatastore.getQueuedRequestCount(), workerLagMs, elbWorkerLagMs,connectionStateString, globalStateNodeSize);
    } else {
      return new BaragonServiceStatus(leaderLatch.hasLeadership(), 0, workerLagMs, elbWorkerLagMs, connectionStateString, 0);
    }
  }

  public BaragonServiceStatus getMasterServiceStatus() {
    if (leaderLatch.hasLeadership()) {
      return getServiceStatus();
    } else {
      try {
        String id = leaderLatch.getLeader().getId();
        Optional<String> maybeLeaderUri = workerDatastore.getBaseUri(id);
        if (maybeLeaderUri.isPresent()) {
          Response response = httpClient.prepareGet(String.format("%s/status", maybeLeaderUri.get())).execute().get();
          return objectMapper.readValue(response.getResponseBody(), BaragonServiceStatus.class);
        } else {
          LOG.warn(String.format("Couldn't get base uri for leader with id %s, returning status from this instance", id));
          return getServiceStatus();
        }
      } catch (Exception e) {
        LOG.error("Error fetching status from leader", e);
        return getServiceStatus();
      }
    }
  }
}

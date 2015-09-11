package com.hubspot.baragon.service.managers;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonServiceStatus;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;

@Singleton
public class StatusManager {
  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong workerLastStart;
  private final AtomicLong elbWorkerLastStart;
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public StatusManager(BaragonRequestDatastore requestDatastore,
                       BaragonStateDatastore stateDatastore,
                       @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                       @Named(BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStart,
                       @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong elbWorkerLastStart,
                       @Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
    this.workerLastStart = workerLastStart;
    this.elbWorkerLastStart = elbWorkerLastStart;
    this.connectionState = connectionState;
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
}

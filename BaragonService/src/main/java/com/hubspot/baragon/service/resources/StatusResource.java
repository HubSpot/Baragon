package com.hubspot.baragon.service.resources;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.BaragonServiceModule;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final BaragonRequestDatastore datastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong workerLastStart;
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public StatusResource(BaragonRequestDatastore datastore,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStart,
                        @Named(BaragonBaseModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.datastore = datastore;
    this.leaderLatch = leaderLatch;
    this.workerLastStart = workerLastStart;
    this.connectionState = connectionState;
  }

  @GET
  public BaragonServiceStatus getServiceStatus() {
    final ConnectionState currentConnectionState = connectionState.get();
    final String connectionStateString = currentConnectionState == null ? "UNKNOWN" : currentConnectionState.name();

    return new BaragonServiceStatus(leaderLatch.hasLeadership(), datastore.getQueuedRequestCount(), System.currentTimeMillis() - workerLastStart.get(), connectionStateString);
  }
}

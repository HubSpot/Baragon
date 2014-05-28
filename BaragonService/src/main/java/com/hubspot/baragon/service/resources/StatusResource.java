package com.hubspot.baragon.service.resources;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.BaragonServiceModule;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.atomic.AtomicLong;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final BaragonRequestDatastore datastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong workerLastStart;

  @Inject
  public StatusResource(BaragonRequestDatastore datastore,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_WORKER_LAST_START) AtomicLong workerLastStart) {
    this.datastore = datastore;
    this.leaderLatch = leaderLatch;
    this.workerLastStart = workerLastStart;
  }

  @GET
  public BaragonServiceStatus getServiceStatus() {
    return new BaragonServiceStatus(leaderLatch.hasLeadership(), datastore.getQueuedRequestCount(), System.currentTimeMillis() - workerLastStart.get());
  }
}

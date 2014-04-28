package com.hubspot.baragon.service.resources;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.service.models.ServiceStatus;
import com.hubspot.baragon.service.BaragonServiceModule;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private final BaragonRequestDatastore datastore;
  private final LeaderLatch leaderLatch;

  @Inject
  public StatusResource(BaragonRequestDatastore datastore,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch) {
    this.datastore = datastore;
    this.leaderLatch = leaderLatch;
  }

  @GET
  public ServiceStatus getServiceStatus() {
    return new ServiceStatus(leaderLatch.hasLeadership(), datastore.getPendingRequestIds().size());
  }
}

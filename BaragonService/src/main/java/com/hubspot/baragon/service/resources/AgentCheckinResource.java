package com.hubspot.baragon.service.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.TrafficSourceState;
import com.hubspot.baragon.service.gcloud.GoogleCloudManager;
import com.hubspot.baragon.service.managers.ElbManager;

@Path("/checkin")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class AgentCheckinResource {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCheckinResource.class);

  private final ElbManager elbManager;
  private final GoogleCloudManager googleCloudManager;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public AgentCheckinResource(ElbManager elbManager,
                              GoogleCloudManager googleCloudManager,
                              BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.elbManager = elbManager;
    this.googleCloudManager = googleCloudManager;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @POST
  @Path("/{clusterName}/startup")
  public AgentCheckInResponse addAgent(@PathParam("clusterName") String clusterName,
                                       @QueryParam("status") boolean status,
                                       BaragonAgentMetadata agent) {
    LOG.info(String.format("Notified of startup for agent %s", agent.getAgentId()));
    AgentCheckInResponse response;
    try {
      if (elbManager.isElbConfigured()) {
        response = elbManager.attemptAddAgent(agent, loadBalancerDatastore.getLoadBalancerGroup(clusterName), clusterName, status);
      } else if (googleCloudManager.isConfigured()) {
        response = googleCloudManager.checkHealthOfAgentOnStartup(agent);
      } else {
        response = new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent startup", e);
      response = new AgentCheckInResponse(TrafficSourceState.ERROR, Optional.of(e.getMessage()), 0L);
    }
    return response;
  }

  @POST
  @Path("/{clusterName}/shutdown")
  public AgentCheckInResponse removeAgent(@PathParam("clusterName") String clusterName,
                                          @QueryParam("status") boolean status,
                                          BaragonAgentMetadata agent) {
    LOG.info(String.format("Notified of shutdown for agent %s", agent.getAgentId()));
    AgentCheckInResponse response;
    try {
      if (elbManager.isElbConfigured()) {
        response = elbManager.attemptRemoveAgent(agent, loadBalancerDatastore.getLoadBalancerGroup(clusterName), clusterName, status);
      } else if (googleCloudManager.isConfigured()) {
        response = googleCloudManager.checkHealthOfAgentOnShutdown(agent);
      } else {
        response = new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent shutdown", e);
      response = new AgentCheckInResponse(TrafficSourceState.ERROR, Optional.of(e.getMessage()), 0L);
    }
    return response;
  }

  @GET
  @NoAuth
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/{clusterName}/can-shutdown")
  public String canShutdownAgent(@PathParam("clusterName") String clusterName, @QueryParam("agentId") String agentId) {
    Optional<BaragonAgentMetadata> maybeAgent = loadBalancerDatastore.getAgent(clusterName, agentId);
    Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(clusterName);
    if (maybeAgent.isPresent()) {
      if (elbManager.isElbEnabledAgent(maybeAgent.get(), maybeGroup, clusterName)) {
        if (elbManager.isActiveAndHealthy(maybeGroup, maybeAgent.get())) {
          return "0";
        }
      }
    }
    return "1";
  }
}

package com.hubspot.baragon.service.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.service.managers.ElbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/checkin")
@Consumes({MediaType.APPLICATION_JSON})
@Produces(MediaType.APPLICATION_JSON)
public class AgentCheckinResource {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCheckinResource.class);

  private final ElbManager elbManager;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public AgentCheckinResource(ElbManager elbManager,
                              BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.elbManager = elbManager;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @POST
  @Path("/{clusterName}/startup")
  public Response addAgent(@PathParam("clusterName") String clusterName, BaragonAgentMetadata agent) {
    LOG.info(String.format("Notified of startup for agent %s", agent.getAgentId()));
    try {
      if (elbManager.isElbConfigured()) {
        elbManager.attemptAddAgent(agent, loadBalancerDatastore.getLoadBalancerGroup(clusterName), clusterName);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent startup", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return Response.ok().build();
  }

  @POST
  @Path("/{clusterName}/shutdown")
  public Response removeAgent(@PathParam("clusterName") String clusterName, BaragonAgentMetadata agent) {
    LOG.info(String.format("Notified of shutdown for agent %s", agent.getAgentId()));
    try {
      if (elbManager.isElbConfigured()) {
        elbManager.attemptRemoveAgent(agent, loadBalancerDatastore.getLoadBalancerGroup(clusterName), clusterName);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent shutdown", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return Response.ok().build();
  }

  @GET
  @NoAuth
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/{clusterName}/can-shutdown")
  public String canShutdownAgent(@PathParam("clusterName") String clusterName, @QueryParam("agentId") String agentId) {
    Optional<BaragonAgentMetadata> maybeAgent = loadBalancerDatastore.getAgent(clusterName, agentId);
    Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(clusterName);
    if (maybeAgent.isPresent()) {
      if (elbManager.elbEnabledAgent(maybeAgent.get(), maybeGroup, clusterName)) {
        if (elbManager.isActiveAndHealthy(maybeGroup, maybeAgent.get())) {
          return "0";
        }
      }
    }
    return "1";
  }
}

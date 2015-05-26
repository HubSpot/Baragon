package com.hubspot.baragon.service.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.managers.ElbManager;
import com.hubspot.baragon.models.BaragonAgentMetadata;
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
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
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
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    return Response.ok().build();
  }
}

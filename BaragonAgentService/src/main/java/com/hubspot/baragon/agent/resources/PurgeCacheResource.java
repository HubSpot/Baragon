package com.hubspot.baragon.agent.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.managers.AgentRequestManager;

@Path("/purgeCache")
@Produces(MediaType.APPLICATION_JSON)
public class PurgeCacheResource {
  private static final Logger LOG = LoggerFactory.getLogger(PurgeCacheResource.class);
  private final AgentRequestManager agentRequestManager;

  @Inject
  public PurgeCacheResource(AgentRequestManager agentRequestManager) {
    this.agentRequestManager = agentRequestManager;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/{serviceId}")
  public Response apply(@PathParam("serviceId") String serviceId) throws InterruptedException {
    return agentRequestManager.purgeCache(serviceId);
  }
}

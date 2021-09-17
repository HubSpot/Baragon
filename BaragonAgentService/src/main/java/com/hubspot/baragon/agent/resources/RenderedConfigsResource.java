package com.hubspot.baragon.agent.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.auth.NoAuth;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/renderedConfigs")
@Produces(MediaType.APPLICATION_JSON)
public class RenderedConfigsResource {
  private static final Logger LOG = LoggerFactory.getLogger(
    RenderedConfigsResource.class
  );

  private final AgentRequestManager agentRequestManager;

  @Inject
  public RenderedConfigsResource(AgentRequestManager agentRequestManager) {
    this.agentRequestManager = agentRequestManager;
  }

  @GET
  @NoAuth
  @Path("/{serviceId}")
  public Response getServiceId(@PathParam("serviceId") String serviceId) {
    LOG.info("Received request to view the renderedConfig of serviceId={}", serviceId);
    return agentRequestManager.getRenderedConfigs(serviceId);
  }
}

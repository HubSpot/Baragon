package com.hubspot.baragon.agent.resources;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.managers.AgentRequestManager;
import com.hubspot.baragon.models.AgentBatchResponseItem;
import com.hubspot.baragon.models.BaragonRequestBatchItem;

@Path("/batch")
@Produces(MediaType.APPLICATION_JSON)
public class BatchRequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(BatchRequestResource.class);
  private final AgentRequestManager agentRequestManager;

  @Inject
  public BatchRequestResource(AgentRequestManager agentRequestManager) {
    this.agentRequestManager = agentRequestManager;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Set<AgentBatchResponseItem> apply(Set<BaragonRequestBatchItem> batch) throws InterruptedException {
    return agentRequestManager.processRequests(batch);
  }
}

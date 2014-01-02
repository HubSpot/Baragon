package com.hubspot.baragon.agent.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.BaragonAgentManager;
import com.hubspot.baragon.agent.LeaderRedirector;
import com.hubspot.baragon.models.ServiceSnapshot;
import com.hubspot.baragon.utils.LogUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Map;

@Path("/external")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalResources {
  private static final Log LOG = LogFactory.getLog(ExternalResources.class);
  
  private final BaragonAgentManager agentManager;
  private final LeaderRedirector leaderRedirector;
  
  @Inject
  public ExternalResources(BaragonAgentManager agentManager, LeaderRedirector leaderRedirector) {
    this.agentManager = agentManager;
    this.leaderRedirector = leaderRedirector;
  }
  
  @Path("/configs")
  @GET
  public Map<String, Boolean> checkConfigs() {
    leaderRedirector.redirectToLeader();

    LOG.info("Asking agentManager to check configs");
    return agentManager.checkConfigs();
  }
  
  @Path("/configs")
  @POST
  public void applyConfig(ServiceSnapshot snapshot) throws Exception {
    leaderRedirector.redirectToLeader();

    LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "Asking agentManager to apply upstreams: ", LogUtils.COMMA_JOINER.join(snapshot.getHealthyUpstreams()));
    agentManager.apply(snapshot);
  }
  
  @Path("/configs/{serviceName}")
  @DELETE
  public void removeProject(@PathParam("serviceName") String serviceName) throws Exception {
    leaderRedirector.redirectToLeader();

    LOG.info("Asking agentManager to remove " + serviceName);
    throw new NotImplementedException("TODO: agentManager.remove()");
  }
  
  @Path("/cluster")
  @GET
  public Collection<String> getCluster() {
    leaderRedirector.redirectToLeader();

    LOG.info("Asking agentManager for cluster info");
    return agentManager.getCluster();
  }
}

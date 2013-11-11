package com.hubspot.baragon.agent.resources;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Joiner;
import com.hubspot.baragon.agent.BaragonAgentManager;
import com.hubspot.baragon.agent.LeaderRedirector;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;

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
  public void applyConfig(ServiceInfoAndUpstreams serviceInfoAndUpstreams) throws Exception {
    leaderRedirector.redirectToLeader();

    LOG.info("Asking agentManager to apply " + serviceInfoAndUpstreams.getServiceInfo().getName());
    LOG.info("   Upstreams: " + Joiner.on(", ").join(serviceInfoAndUpstreams.getUpstreams()));
    agentManager.apply(serviceInfoAndUpstreams.getServiceInfo(), serviceInfoAndUpstreams.getUpstreams());
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

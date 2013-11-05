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
import javax.ws.rs.core.MediaType;

import com.google.common.base.Joiner;
import com.hubspot.baragon.agent.BaragonAgentManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.lbs.models.ServiceInfoAndUpstreams;

@Path("/external")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalResources {
  private static final Log LOG = LogFactory.getLog(ExternalResources.class);
  
  private final BaragonAgentManager coordinator;
  
  @Inject
  public ExternalResources(BaragonAgentManager coordinator) {
    this.coordinator = coordinator;
  }
  
  @Path("/configs")
  @GET
  public Map<String, Boolean> checkConfigs() {
    LOG.info("Asking coordinator to check configs");
    return coordinator.checkConfigs();
  }
  
  @Path("/configs")
  @POST
  public void applyConfig(ServiceInfoAndUpstreams serviceInfoAndUpstreams) throws Exception {
    LOG.info("Asking coordinator to apply " + serviceInfoAndUpstreams.getServiceInfo().getName());
    LOG.info("   Upstreams: " + Joiner.on(", ").join(serviceInfoAndUpstreams.getUpstreams()));
    coordinator.apply(serviceInfoAndUpstreams.getServiceInfo(), serviceInfoAndUpstreams.getUpstreams());
  }
  
  @Path("/configs/{serviceName}")
  @DELETE
  public void removeProject(@PathParam("serviceName") String serviceName) throws Exception {
    LOG.info("Asking coordinator to remove " + serviceName);
    // TODO remove
  }
  
  @Path("/cluster")
  @GET
  public Collection<String> getCluster() {
    LOG.info("Asking coordinator for cluster info");
    return coordinator.getCluster();
  }
}

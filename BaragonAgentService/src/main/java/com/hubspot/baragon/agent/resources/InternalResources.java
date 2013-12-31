package com.hubspot.baragon.agent.resources;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.models.AgentStatus;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalResources {
  private static final Log LOG = LogFactory.getLog(InternalResources.class);
  
  private final LbConfigHelper configHelper;
  private final LbAdapter adapter;
  private final BaragonDataStore datastore;
  private final LeaderLatch leaderLatch;
  private final AtomicLong lastRun;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  
  @Inject
  public InternalResources(BaragonDataStore datastore, LbConfigHelper configHelper, LbAdapter adapter,
                           LeaderLatch leaderLatch, @Named(BaragonAgentServiceModule.POLLER_LAST_RUN) AtomicLong lastRun,
                           LoadBalancerConfiguration loadBalancerConfiguration) {
    this.configHelper = configHelper;
    this.adapter = adapter;
    this.datastore = datastore;
    this.leaderLatch = leaderLatch;
    this.lastRun = lastRun;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }
  
  @Path("/configs/{serviceName}")
  @DELETE
  public void removeConfig(@PathParam("serviceName") String serviceName) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);
    if (!maybeServiceInfo.isPresent()) {
      throw new WebApplicationException(404);
    }

    LOG.info("Going to remove configs for " + serviceName);
    
    configHelper.remove(maybeServiceInfo.get());

    LOG.info("Finished removing " + serviceName);
  }
  
  @Path("/configs/{serviceName}/rollback")
  @POST
  public void rollbackConfigs(@PathParam("serviceName") String serviceName) {
    Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);
    if (!maybeServiceInfo.isPresent()) {
      throw new WebApplicationException(404);
    }

    LOG.info("Going to rollback configs for " + serviceName);
    
    configHelper.restoreConfigs(maybeServiceInfo.get());
    
    LOG.info("Finished rolling back configs for " + serviceName);
  }
  
  @Path("/configs")
  @POST
  public void applyConfig(ServiceInfoAndUpstreams serviceInfoAndUpstreams) throws Exception {
    LOG.info("Going to apply config " + serviceInfoAndUpstreams.getServiceInfo().getName());

    Collection<String> upstreams = serviceInfoAndUpstreams.getUpstreams();
    LOG.info("   Upstreams for " + serviceInfoAndUpstreams.getServiceInfo().getName() + ": " + Joiner.on(", ").join(upstreams));
    
    configHelper.apply(serviceInfoAndUpstreams.getServiceInfo(), upstreams);
    
    LOG.info("Finished applying " + serviceInfoAndUpstreams.getServiceInfo().getName());
  }
  
  @Path("/configs/check")
  @GET
  public void checkConfigs() {
    LOG.info("Going to check configs");
    
    adapter.checkConfigs();
    
    LOG.info("Finished checking configs");
  }

  @Path("/status")
  @GET
  public AgentStatus getAgentStatus() {
    boolean validConfigs = true;

    try {
      adapter.checkConfigs();
    } catch (Exception e) {
      validConfigs = false;
    }

    return new AgentStatus(loadBalancerConfiguration.getName(), leaderLatch.hasLeadership(),
        System.currentTimeMillis() - lastRun.get(), validConfigs);
  }
}

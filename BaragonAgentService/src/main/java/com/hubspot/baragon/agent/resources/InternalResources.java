package com.hubspot.baragon.agent.resources;

import java.util.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;

@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalResources {
  private static final Log LOG = LogFactory.getLog(InternalResources.class);
  
  private final LbConfigHelper configHelper;
  private final LbAdapter adapter;
  private final BaragonDataStore datastore;
  
  @Inject
  public InternalResources(BaragonDataStore datastore, LbConfigHelper configHelper, LbAdapter adapter) {
    this.configHelper = configHelper;
    this.adapter = adapter;
    this.datastore = datastore;
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
}

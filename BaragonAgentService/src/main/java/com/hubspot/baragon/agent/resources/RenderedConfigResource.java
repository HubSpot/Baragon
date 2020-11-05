package com.hubspot.baragon.agent.resources;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BasicServiceContext;

@Path("/renderedConfig")
@Produces(MediaType.APPLICATION_JSON)
public class RenderedConfigResource {
  private static final Logger LOG = LoggerFactory.getLogger(RenderedConfigResource.class);

  private final Map<String, BasicServiceContext> internalStateCache;
  private final FilesystemConfigHelper filesystemConfigHelper;
  private final BaragonStateDatastore baragonStateDatastore;

  @Inject
  public RenderedConfigResource(
                               FilesystemConfigHelper filesystemConfigHelper,
                               @Named(BaragonAgentServiceModule.INTERNAL_STATE_CACHE) Map<String, BasicServiceContext> internalStateCache,
                               BaragonStateDatastore baragonStateDatastore) {

    this.internalStateCache = internalStateCache;
    this.filesystemConfigHelper = filesystemConfigHelper;
    this.baragonStateDatastore = baragonStateDatastore;
  }

  @GET
  @Path("/{serviceId}")
  public Collection<BaragonConfigFile> getServiceId(@PathParam("serviceId") String serviceId) {
    LOG.info("Received request to view the renderedConfig of serviceId={}", serviceId);
    if (internalStateCache.containsKey(serviceId)){
      return filesystemConfigHelper.readConfigs(internalStateCache.get(serviceId).getService());
    }
    else {
      Optional<BaragonService> maybeService = baragonStateDatastore.getService(serviceId);
      if (maybeService.isPresent()){
        return filesystemConfigHelper.readConfigs(maybeService.get());
      }
    }
    return Collections.EMPTY_LIST;
  }
}


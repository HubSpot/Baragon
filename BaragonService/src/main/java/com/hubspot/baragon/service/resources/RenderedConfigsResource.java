package com.hubspot.baragon.service.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.service.managers.RenderedConfigsManager;

@Path("/renderedConfigs")
@Produces(MediaType.APPLICATION_JSON)
public class RenderedConfigsResource {

  private final RenderedConfigsManager renderedConfigsManager;

  @Inject
  public RenderedConfigsResource(RenderedConfigsManager renderedConfigsManager){
    this.renderedConfigsManager = renderedConfigsManager;
  }


  @GET
  @NoAuth
  @Path("/{serviceId}")
  public List<BaragonConfigFile> getRenderedConfigs(@PathParam("serviceId") String serviceId) throws Exception {
    return renderedConfigsManager.synchronouslySendRenderConfigsRequest(serviceId);
  }
}

package com.hubspot.baragon.service.resources;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@Path("/allowuiwrite")
public class UIAuthResource {
  private final BaragonConfiguration configuration;

  @Inject
  public UIAuthResource(@Named(BaragonServiceModule.BARAGON_URI_BASE) String baragonUriBase, BaragonConfiguration configuration) {
    this.configuration = configuration;
  }

  @GET
  @Path("/")
  @Produces(MediaType.TEXT_PLAIN)
  public String allowsWrite (@QueryParam("uiAuthKey") String uiAuthKey){
    Optional<String> maybeAllowEditKey = configuration.getUiConfiguration().getAllowEditKey();
    if (maybeAllowEditKey.isPresent() && maybeAllowEditKey.get().equals(uiAuthKey)) {
      return "allowed";
    } else {
      return "No matching keys";
    }
  }
}

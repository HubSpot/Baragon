package com.hubspot.baragon.service.resources;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.views.IndexView;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@Path(UIResource.UI_RESOURCE_LOCATION + "{uiPath:.*}")
@NoAuth
public class UIResource {

  static final String UI_RESOURCE_LOCATION = "/ui";

  private final BaragonConfiguration configuration;
  private final String baragonUriBase;

  @Inject
  public UIResource(@Named(BaragonServiceModule.BARAGON_URI_BASE) String baragonUriBase, BaragonConfiguration configuration) {
    this.configuration = configuration;
    this.baragonUriBase = baragonUriBase;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public IndexView getIndex() {
    return new IndexView(baragonUriBase, UI_RESOURCE_LOCATION, configuration);
  }
}

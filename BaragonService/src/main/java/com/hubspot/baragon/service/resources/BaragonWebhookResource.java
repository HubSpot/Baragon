package com.hubspot.baragon.service.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaragonWebhookResource {
  private BaragonDataStore datastore;

  @Inject
  public BaragonWebhookResource(BaragonDataStore datastore) {
    this.datastore = datastore;
  }

  @POST
  public void addWebhook(@QueryParam("url") String url) {
    datastore.addWebhook(url);
  }

  @GET
  public Iterable<String> getWebhooks() {
    return datastore.getWebhooks();
  }

  @DELETE
  public void removeWebhook(@QueryParam("url") String url) {
    datastore.removeWebhook(url);
  }
}

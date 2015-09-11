package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonWorkerDatastore;

@Path("/workers")
@Produces(MediaType.APPLICATION_JSON)
@NoAuth
public class WorkerResource {
  private final BaragonWorkerDatastore datastore;

  @Inject
  public WorkerResource(BaragonWorkerDatastore datastore) {
    this.datastore = datastore;
  }

  @GET
  public Collection<String> getWorkers() {
    return datastore.getBaseUris();
  }
}

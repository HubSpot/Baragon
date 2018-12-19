package com.hubspot.baragon.service.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.models.BaragonGroupAlias;

@Path("/aliases")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AliasesResource {
  private final BaragonAliasDatastore aliasDatastore;

  @Inject
  public AliasesResource(BaragonAliasDatastore aliasDatastore) {
    this.aliasDatastore = aliasDatastore;
  }

  @POST
  @Path("/{name}")
  public BaragonGroupAlias createAlias(@PathParam("name") String name, BaragonGroupAlias alias) {
    aliasDatastore.saveAlias(name, alias);
    return alias;
  }

  @GET
  @Path("/{name}")
  public Optional<BaragonGroupAlias> getAlias(@PathParam("name") String name) {
    return aliasDatastore.getAlias(name);
  }

  @DELETE
  @Path("/{name}")
  public void deleteAlias(@PathParam("name") String name) {
    aliasDatastore.deleteAlias(name);
  }
}

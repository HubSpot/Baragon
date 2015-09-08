package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.service.BaragonServiceModule;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
  private final BaragonAuthDatastore datastore;
  private final String masterAuthKey;

  @Inject
  public AuthResource(BaragonAuthDatastore datastore,
                      @Named(BaragonServiceModule.BARAGON_MASTER_AUTH_KEY) String masterAuthKey) {
    this.datastore = datastore;
    this.masterAuthKey = masterAuthKey;
  }

  @GET
  @Path("/key/verify")
  public void verifyKey() {

  }

  @GET
  @NoAuth
  @Path("/keys")
  public Collection<BaragonAuthKey> getKeys(@QueryParam("authkey") String queryAuthKey) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    return datastore.getAuthKeyMap().values();
  }

  @DELETE
  @NoAuth
  @Path("/keys/{key}")
  public Optional<BaragonAuthKey> expireKey(@PathParam("key") String key, @QueryParam("authkey") String queryAuthKey) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    return datastore.expireAuthKey(key);
  }

  @POST
  @NoAuth
  @Path("/keys")
  public void addKey(BaragonAuthKey authKey, @QueryParam("authkey") String queryAuthKey) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    datastore.addAuthKey(authKey);
  }
}

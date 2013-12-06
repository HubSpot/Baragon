package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.exceptions.MissingLoadBalancersException;
import com.hubspot.baragon.exceptions.PendingServiceOccupiedException;
import com.hubspot.baragon.service.BaragonServiceManager;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaragonServiceResource {
  private static final Log LOG = LogFactory.getLog(BaragonServiceResource.class);

  private final BaragonServiceManager baragonDeployManager;

  @Inject
  public BaragonServiceResource(BaragonServiceManager baragonDeployManager) {
    this.baragonDeployManager = baragonDeployManager;
  }

  @POST
  public void addPendingService(ServiceInfo serviceInfo) {
    try {
      baragonDeployManager.addPendingService(serviceInfo);
    } catch (PendingServiceOccupiedException e) {
      throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
          .entity(e.getPendingService())
          .build());
    } catch (MissingLoadBalancersException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(e)
          .build());
    }
  }

  @POST
  @Path("/{serviceName}/activate")
  public Optional<ServiceInfo> activateService(@PathParam("serviceName") String serviceName) {
    return baragonDeployManager.activateService(serviceName);
  }

  @DELETE
  @Path("/{serviceName}")
  public void teardown(@PathParam("serviceName") String serviceName) {
    // TODO: implement
  }

  @DELETE
  @Path("/{serviceName}/pending")
  public Optional<ServiceInfo> removePendingService(@PathParam("serviceName") String serviceName) {
    Optional<ServiceInfo> maybeServiceInfo = baragonDeployManager.getPendingService(serviceName);

    if (maybeServiceInfo.isPresent()) {
      baragonDeployManager.removePendingService(serviceName);
    }

    return maybeServiceInfo;
  }

  @GET
  @Path("/{serviceName}/active")
  public Optional<ServiceInfo> getActiveService(@PathParam("serviceName") String serviceName) {
    return baragonDeployManager.getActiveService(serviceName);
  }

  @GET
  @Path("/{serviceName}/pending")
  public Optional<ServiceInfo> getPendingService(@PathParam("serviceName") String serviceName) {
    return baragonDeployManager.getPendingService(serviceName);
  }

  @POST
  @Path("/{serviceName}/sync")
  public void syncUpstreams(@PathParam("serviceName") String serviceName) {
    baragonDeployManager.syncUpstreams(serviceName);
  }
}

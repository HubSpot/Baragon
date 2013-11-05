package com.hubspot.baragon.service.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceManager;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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
  public void add(ServiceInfo serviceInfo) {
    baragonDeployManager.addService(serviceInfo);
  }

  @POST
  @Path("/{serviceName}/activate")
  public void activateDeploy(@PathParam("serviceName") String serviceName) {
    baragonDeployManager.activateService(serviceName);
  }

  @DELETE
  @Path("/{serviceName}")
  public void teardown(@PathParam("serviceName") String serviceName) {
    // TODO: implement
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
  public void runHealthcheck(@PathParam("serviceName") String serviceName) {
    baragonDeployManager.syncUpstreams(serviceName);
  }
}

package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonService;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoadBalancerResource {
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonStateDatastore stateDatastore;

  @Inject
  public LoadBalancerResource(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              BaragonKnownAgentsDatastore knownAgentsDatastore,
                              BaragonStateDatastore stateDatastore) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.stateDatastore = stateDatastore;
  }

  @GET
  public Collection<String> getClusters() {
    return loadBalancerDatastore.getClusters();
  }
  
  @GET
  @Path("/{clusterName}/hosts")
  @Deprecated
  public Collection<String> getHosts(@PathParam("clusterName") String clusterName) {
    final Collection<BaragonAgentMetadata> agentMetadatas = loadBalancerDatastore.getAgentMetadata(clusterName);
    final Collection<String> baseUris = Lists.newArrayListWithCapacity(agentMetadatas.size());

    for (BaragonAgentMetadata agentMetadata : agentMetadatas) {
      baseUris.add(agentMetadata.getBaseAgentUri());
    }

    return baseUris;
  }

  @GET
  @Path("/{clusterName}/agents")
  public Collection<BaragonAgentMetadata> getAgentMetadata(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getAgentMetadata(clusterName);
  }

  @GET
  @Path("/{clusterName}/known-agents")
  public Collection<BaragonAgentMetadata> getKnownAgentsMetadata(@PathParam("clusterName") String clusterName) {
    return knownAgentsDatastore.getKnownAgentsMetadata(clusterName);
  }

  @DELETE
  @Path("/{clusterName}/known-agents/{agentId}")
  public void deleteKnownAgent(@PathParam("clusterName") String clusterName, @PathParam("agentId") String agentId) {
    knownAgentsDatastore.removeKnownAgent(clusterName, agentId);
  }

  @GET
  @Path("/{clusterName}/base-path/all")
  public Collection<String> getBasePaths(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getBasePaths(clusterName);
  }

  @GET
  @Path("/{clusterName}/base-path")
  public Optional<BaragonService> getBasePathServiceId(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(clusterName, basePath);

    if (!maybeServiceId.isPresent()) {
      return Optional.absent();
    }

    return stateDatastore.getService(maybeServiceId.get());
  }

  @DELETE
  @Path("/{clusterName}/base-path")
  public void clearBasePath(@PathParam("clusterName") String clusterName, @QueryParam("basePath") String basePath) {
    loadBalancerDatastore.clearBasePath(clusterName, basePath);
  }
}

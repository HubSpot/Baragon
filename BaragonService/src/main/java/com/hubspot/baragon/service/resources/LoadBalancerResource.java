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
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.sun.javaws.exceptions.InvalidArgumentException;

@Path("/load-balancer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoadBalancerResource {
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonConfiguration configuration;

  @Inject
  public LoadBalancerResource(BaragonLoadBalancerDatastore loadBalancerDatastore,
                              BaragonKnownAgentsDatastore knownAgentsDatastore,
                              BaragonStateDatastore stateDatastore,
                              BaragonConfiguration configuration) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.stateDatastore = stateDatastore;
    this.configuration = configuration;
  }

  @GET
  @NoAuth
  public Collection<String> getClusters() {
    return loadBalancerDatastore.getLoadBalancerGroupNames();
  }

  @GET
  @NoAuth
  @Path("/{clusterName}")
  public Optional<BaragonGroup> getGroupDetail(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getLoadBalancerGroup(clusterName);
  }

  @POST
  @Path("/{clusterName}/sources")
  public BaragonGroup addSource(@PathParam("clusterName") String clusterName, @QueryParam("source") String source) {
    return loadBalancerDatastore.addSourceToGroup(clusterName, source);
  }

  @DELETE
  @Path("/{clusterName}/sources")
  public Optional<BaragonGroup> removeSource(@PathParam("clusterName") String clusterName, @QueryParam("source") String source) {
    return loadBalancerDatastore.removeSourceFromGroup(clusterName, source);
  }

  @POST
  @Path("/{clusterName}/count")
  public Integer setTargetCount(@PathParam("clusterName") String clusterName, @QueryParam("count") int count) {
    return loadBalancerDatastore.setTargetCount(clusterName, count);
  }

  @GET
  @Path("/{clusterName}/count")
  public Integer getTargetCount(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getTargetCount(clusterName).or(configuration.getDefaultTargetAgentCount());
  }

  @GET
  @NoAuth
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
  @NoAuth
  @Path("/{clusterName}/agents")
  public Collection<BaragonAgentMetadata> getAgentMetadata(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getAgentMetadata(clusterName);
  }

  @GET
  @NoAuth
  @Path("/{clusterName}/known-agents")
  public Collection<BaragonKnownAgentMetadata> getKnownAgentsMetadata(@PathParam("clusterName") String clusterName) {
    return knownAgentsDatastore.getKnownAgentsMetadata(clusterName);
  }

  @DELETE
  @Path("/{clusterName}/known-agents/{agentId}")
  public void deleteKnownAgent(@PathParam("clusterName") String clusterName, @PathParam("agentId") String agentId) {
    knownAgentsDatastore.removeKnownAgent(clusterName, agentId);
  }

  @GET
  @NoAuth
  @Path("/{clusterName}/base-path/all")
  public Collection<String> getBasePaths(@PathParam("clusterName") String clusterName) {
    return loadBalancerDatastore.getBasePaths(clusterName);
  }

  @GET
  @NoAuth
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

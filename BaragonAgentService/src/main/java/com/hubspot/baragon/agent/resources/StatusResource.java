package com.hubspot.baragon.agent.resources;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.models.AgentStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
  private static final Log LOG = LogFactory.getLog(StatusResource.class);

  private final LocalLbAdapter adapter;
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public StatusResource(LocalLbAdapter adapter, LoadBalancerConfiguration loadBalancerConfiguration) {
    this.adapter = adapter;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  @GET
  public AgentStatus getStatus() {
    boolean validConfigs = true;

    try {
      adapter.checkConfigs();
    } catch (InvalidConfigException e) {
      validConfigs = false;
      LOG.warn("Configs are invalid", e);
    }

    return new AgentStatus(loadBalancerConfiguration.getName(), validConfigs);
  }
}

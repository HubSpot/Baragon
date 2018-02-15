package com.hubspot.baragon.service.gcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.BackendServiceGroupHealth;
import com.google.api.services.compute.model.HealthStatus;
import com.google.api.services.compute.model.ResourceGroupReference;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.models.BaragonAgentGcloudMetadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.TrafficSourceState;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.GoogleCloudConfiguration;

public class GoogleCloudManager {
  private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudManager.class);
  private static final String HEALTHY_STATE = "HEALTHY";

  private final Compute compute;
  private final GoogleCloudConfiguration googleCloudConfiguration;

  @Inject
  public GoogleCloudManager(BaragonConfiguration configuration,
                            @Named(BaragonServiceModule.GOOGLE_CLOUD_COMPUTE_SERVICE) Optional<Compute> compute)  {
    this.compute = compute.orNull();
    this.googleCloudConfiguration = configuration.getGoogleCloudConfiguration();
  }

  public boolean isConfigured() {
    return googleCloudConfiguration.isEnabled();
  }

  public AgentCheckInResponse checkHealthOfAgentOnStartup(BaragonAgentMetadata agent) {
    return checkAgent(agent, true);
  }

  public AgentCheckInResponse checkHealthOfAgentOnShutdown(BaragonAgentMetadata agent) {
    return checkAgent(agent, false);
  }

  private AgentCheckInResponse checkAgent(BaragonAgentMetadata agent, boolean startup) {
    if (!agent.getGcloud().isPresent()) {
      return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0);
    }

    BaragonAgentGcloudMetadata gcloudMetadata = agent.getGcloud().get();

    try {
      ResourceGroupReference resourceGroupRef = new ResourceGroupReference().setGroup(gcloudMetadata.getResourceGroup());
      BackendServiceGroupHealth healthResponse;
      if (gcloudMetadata.getRegion().isPresent()) {
        Compute.RegionBackendServices.GetHealth healthRequest = compute.regionBackendServices().getHealth(gcloudMetadata.getProject(), gcloudMetadata.getRegion().get(), gcloudMetadata.getBackendService(), resourceGroupRef);
        healthResponse = healthRequest.execute();
      } else {
        Compute.BackendServices.GetHealth globalHealthRequest = compute.backendServices().getHealth(gcloudMetadata.getProject(), gcloudMetadata.getBackendService(), resourceGroupRef);
        healthResponse = globalHealthRequest.execute();
      }

      if (healthResponse != null && healthResponse.getHealthStatus() != null) {
        for (HealthStatus healthStatus : healthResponse.getHealthStatus()) {
          String instance = healthStatus.getInstance(); // https://www.googleapis.com/compute/beta/projects/(project)/zones/(region)/instances/(instance name)
          if (instance.endsWith(gcloudMetadata.getInstanceName())) {
            if (healthStatus.getHealthState().equals(HEALTHY_STATE)) {
              if (startup) {
                return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0);
              } else {
                return new AgentCheckInResponse(TrafficSourceState.PENDING, Optional.absent(), 5000L);
              }
            } else {
              if (startup) {
                return new AgentCheckInResponse(TrafficSourceState.PENDING, Optional.absent(), 5000L);
              } else {
                return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0);
              }
            }
          }
        }
        LOG.warn("Agent {} not found in instance list for backend service {}", gcloudMetadata.getInstanceName(), gcloudMetadata.getBackendService());
        return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), googleCloudConfiguration.getDefaultCheckInWaitTimeMs());
      } else {
        LOG.warn("No response from gcloud for group {}", resourceGroupRef);
        return new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), googleCloudConfiguration.getDefaultCheckInWaitTimeMs());
      }
    } catch (Throwable t) {
      LOG.error("Exception while checking agent health", t);
      return new AgentCheckInResponse(TrafficSourceState.ERROR, Optional.of(t.getMessage()), googleCloudConfiguration.getDefaultCheckInWaitTimeMs());
    }
  }
}

package com.hubspot.baragon.agent.resources;

import com.google.inject.AbstractModule;

public class BargonAgentResourcesModule extends AbstractModule {
  @Override
  public void configure() {
    bind(BatchRequestResource.class);
    bind(MetricsResource.class);
    bind(RequestResource.class);
    bind(StatusResource.class);
  }
}

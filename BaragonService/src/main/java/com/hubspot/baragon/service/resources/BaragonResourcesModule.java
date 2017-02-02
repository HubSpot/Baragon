package com.hubspot.baragon.service.resources;

import com.google.inject.AbstractModule;

public class BaragonResourcesModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AgentCheckinResource.class);
    bind(AlbResource.class);
    bind(AuthResource.class);
    bind(ElbResource.class);
    bind(LoadBalancerResource.class);
    bind(MetricsResource.class);
    bind(RequestResource.class);
    bind(StateResource.class);
    bind(StatusResource.class);
    bind(UIResource.class);
    bind(WorkerResource.class);
  }
}

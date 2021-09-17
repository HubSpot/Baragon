package com.hubspot.baragon.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.AuthConfiguration;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

@Singleton
public class BaragonAuthFeature implements DynamicFeature {
  private final BaragonAuthFilter requestFilter;
  private final AuthConfiguration authConfiguration;

  @Inject
  public BaragonAuthFeature(
    BaragonAuthFilter requestFilter,
    AuthConfiguration authConfiguration
  ) {
    this.requestFilter = requestFilter;
    this.authConfiguration = authConfiguration;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
    if (authConfiguration.isEnabled()) {
      if (
        resourceInfo.getResourceMethod().getAnnotation(NoAuth.class) == null &&
        resourceInfo.getResourceClass().getAnnotation(NoAuth.class) == null
      ) {
        featureContext.register(requestFilter);
      }
    }
  }
}

package com.hubspot.baragon.service.config;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.styles.HubSpotStyle;

@Immutable
@HubSpotStyle
public interface KubernetesIntegrationConfigurationIF {
  @Default
  default String getAppRootKey() {
    return "appRoot";
  }

  @Default
  default String getServiceNameKey() {
    return "serviceName";
  }

  @Default
  default String getLoadBalancerTemplateKey() {
  return "loadBalancerTemplate";
  }

  @Default
  default String getLbGroupsKey() {
    return "loadBalancerGroups";
  }
}

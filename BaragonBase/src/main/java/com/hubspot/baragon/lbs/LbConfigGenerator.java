package com.hubspot.baragon.lbs;

import com.hubspot.baragon.models.ServiceInfo;

import java.util.Collection;

/**
 * A LbConfigGenerator handles generating LB configs for a specific deploy.
 * @author tpetr
 *
 */
public interface LbConfigGenerator {
  public Collection<LbConfigFile> generateConfigsForProject(ServiceInfo deployInfo, Collection<String> upstreams);
  public Collection<String> getConfigPathsForProject(final ServiceInfo deploy);
}

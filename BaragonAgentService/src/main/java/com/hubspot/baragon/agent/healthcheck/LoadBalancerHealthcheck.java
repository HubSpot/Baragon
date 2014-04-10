package com.hubspot.baragon.agent.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadBalancerHealthcheck extends HealthCheck {
  private static final Log LOG = LogFactory.getLog(LoadBalancerHealthcheck.class);

  private final LocalLbAdapter adapter;
  
  @Inject
  public LoadBalancerHealthcheck(LocalLbAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  protected Result check() throws Exception {
    try {
      adapter.checkConfigs();
      return Result.healthy();
    } catch (Exception e) {
      LOG.warn("Healthcheck failed: " + e.getMessage());
      return Result.unhealthy(e.getMessage());
    }
  }

}

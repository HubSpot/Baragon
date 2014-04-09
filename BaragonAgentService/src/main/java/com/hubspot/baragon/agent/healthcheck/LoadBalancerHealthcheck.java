package com.hubspot.baragon.agent.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LbAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadBalancerHealthcheck extends HealthCheck {
  private static final Log LOG = LogFactory.getLog(LoadBalancerHealthcheck.class);

  private final LbAdapter adapter;
  
  @Inject
  public LoadBalancerHealthcheck(LbAdapter adapter) {
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

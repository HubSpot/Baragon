package com.hubspot.baragon.agent.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;

@Singleton
public class LoadBalancerHealthcheck extends HealthCheck {
  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerHealthcheck.class);

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
      return Result.unhealthy(e);
    }
  }
}

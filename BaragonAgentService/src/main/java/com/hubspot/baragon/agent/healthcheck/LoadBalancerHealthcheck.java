package com.hubspot.baragon.agent.healthcheck;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.dropwizard.guice.InjectableHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadBalancerHealthcheck extends InjectableHealthCheck {
  public static final String NAME = "loadBalancerConfigs";

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

  @Override
  public String getName() {
    return NAME;
  }
}

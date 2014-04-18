package com.hubspot.baragon.agent.healthcheck;

import com.google.inject.Inject;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.dropwizard.guice.InjectableHealthCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadBalancerHealthcheck extends InjectableHealthCheck {
  public static final String NAME = "loadBalancerConfigs";

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
      return Result.unhealthy(e);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }
}

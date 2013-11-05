package com.hubspot.baragon.agent.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LbAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LbHealthcheck extends HealthCheck {
  private static final Log LOG = LogFactory.getLog(LbHealthcheck.class);

  private final LbAdapter adapter;
  
  @Inject
  public LbHealthcheck(LbAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  protected Result check() throws Exception {
    try {
      LOG.info("Health check requested -- checking local configs.");
      adapter.checkConfigs();
      LOG.info("    OK!");
      return Result.healthy();
    } catch (Exception e) {
      LOG.info("    Unhealthy: " + e.getMessage());
      return Result.unhealthy(e.getMessage());
    }
  }

}

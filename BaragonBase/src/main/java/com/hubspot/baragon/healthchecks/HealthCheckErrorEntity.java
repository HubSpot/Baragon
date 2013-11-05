package com.hubspot.baragon.healthchecks;

import java.util.Map;

public class HealthCheckErrorEntity extends RuntimeException {
  private Map<String, HealthCheckServerError> upstreamErrors;

  public HealthCheckErrorEntity(final Map<String, HealthCheckServerError> upstreamErrors) {
    super("Not all of the healthchecks passed.");
    this.upstreamErrors = upstreamErrors;
  }

  public Map<String, HealthCheckServerError> getUpstreamErrors() {
    return upstreamErrors;
  }

  public void setUpstreamErrors(final Map<String, HealthCheckServerError> upstreamErrors) {
    this.upstreamErrors = upstreamErrors;
  }
}

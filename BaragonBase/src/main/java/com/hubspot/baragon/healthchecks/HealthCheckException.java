package com.hubspot.baragon.healthchecks;

import java.util.Map;

public class HealthCheckException extends RuntimeException {
  public HealthCheckException(final Map<String, String> serverErrors) {
    // TODO: make better
  }
}
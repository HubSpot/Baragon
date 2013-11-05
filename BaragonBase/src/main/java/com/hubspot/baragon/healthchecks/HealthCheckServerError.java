package com.hubspot.baragon.healthchecks;

public class HealthCheckServerError {
  private int statusCode;
  private String body;

  public HealthCheckServerError(final int statusCode, final String body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(final int statusCode) {
    this.statusCode = statusCode;
  }

  public String getBody() {
    return body;
  }

  public void setBody(final String body) {
    this.body = body;
  }
}

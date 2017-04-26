package com.hubspot.baragon.models;

import com.google.common.base.MoreObjects;

public class AgentResponseId {
  private final String id;
  private final int statusCode;
  private final boolean exception;
  private final int attempt;

  public static AgentResponseId fromString(String value) {
    final String[] splits = value.split("\\-", 3);

    return new AgentResponseId(value, Integer.parseInt(splits[0]), Boolean.parseBoolean(splits[1]), Integer.parseInt(splits[2]));
  }

  private AgentResponseId(String id, int statusCode, boolean exception, int attempt) {
    this.id = id;
    this.statusCode = statusCode;
    this.exception = exception;
    this.attempt = attempt;
  }

  public String getId() {
    return id;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public boolean isException() {
    return exception;
  }

  public int getAttempt() {
    return attempt;
  }

  public boolean isSuccess() {
    return !exception && statusCode >= 200 && statusCode < 300;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AgentResponseId that = (AgentResponseId) o;

    if (attempt != that.attempt) {
      return false;
    }
    if (exception != that.exception) {
      return false;
    }
    if (statusCode != that.statusCode) {
      return false;
    }
    if (!id.equals(that.id)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + statusCode;
    result = 31 * result + (exception ? 1 : 0);
    result = 31 * result + attempt;
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("statusCode", statusCode)
        .add("exception", exception)
        .add("attempt", attempt)
        .toString();
  }
}

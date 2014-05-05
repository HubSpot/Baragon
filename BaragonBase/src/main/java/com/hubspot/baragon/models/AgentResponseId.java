package com.hubspot.baragon.models;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;

public class AgentResponseId {
  private static final Splitter DASH_SPLITTER = Splitter.on('-');
  private final String id;
  private final int statusCode;
  private final boolean exception;
  private final int attempt;

  public static AgentResponseId fromString(String value) {
    List<String> splits = Lists.newArrayList(DASH_SPLITTER.split(value));

    return new AgentResponseId(value, Integer.parseInt(splits.get(0)), Boolean.parseBoolean(splits.get(1)), Integer.parseInt(splits.get(2)));
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
  public String toString() {
    return Objects.toStringHelper(this)
        .add("statusCode", statusCode)
        .add("exception", exception)
        .add("attempt", attempt)
        .toString();
  }
}

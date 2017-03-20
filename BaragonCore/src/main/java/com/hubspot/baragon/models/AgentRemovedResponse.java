package com.hubspot.baragon.models;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class AgentRemovedResponse {
  private final Optional<Long> connectionDrainTimeMs;
  private final boolean removed;
  private final Optional<String> exceptionMessage;

  @JsonCreator
  public AgentRemovedResponse(@JsonProperty("connectionDrainTimeMs") Optional<Long> connectionDrainTimeMs,
                              @JsonProperty("removed") boolean removed,
                              @JsonProperty("exceptionMessage") Optional<String> exceptionMessage) {
    this.connectionDrainTimeMs = connectionDrainTimeMs;
    this.removed = removed;
    this.exceptionMessage = exceptionMessage;
  }

  public Optional<Long> getConnectionDrainTimeMs() {
    return connectionDrainTimeMs;
  }

  public boolean isRemoved() {
    return removed;
  }

  public Optional<String> getExceptionMessage() {
    return exceptionMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AgentRemovedResponse that = (AgentRemovedResponse) o;
    return removed == that.removed &&
        Objects.equals(connectionDrainTimeMs, that.connectionDrainTimeMs) &&
        Objects.equals(exceptionMessage, that.exceptionMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionDrainTimeMs, removed, exceptionMessage);
  }

  @Override
  public String toString() {
    return "AgentRemovedResponse{" +
        "connectionDrainTimeMs=" + connectionDrainTimeMs +
        ", removed=" + removed +
        ", exceptionMessage=" + exceptionMessage +
        '}';
  }
}

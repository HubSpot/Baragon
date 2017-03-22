package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class AgentCheckInResponse {
  private final Optional<String> exceptionMessage;
  private final TrafficSourceState state;
  private final long waitTime;

  @JsonCreator
  public AgentCheckInResponse(@JsonProperty("state") TrafficSourceState state,
                              @JsonProperty("exceptionMessage") Optional<String> exceptionMessage,
                              @JsonProperty("waitTime") long waitTime) {
    this.state = state;
    this.exceptionMessage = exceptionMessage;
    this.waitTime = waitTime;
  }

  public Optional<String> getExceptionMessage() {
    return exceptionMessage;
  }

  public TrafficSourceState getState() {
    return state;
  }

  public long getWaitTime() {
    return waitTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AgentCheckInResponse that = (AgentCheckInResponse) o;

    if (waitTime != that.waitTime) {
      return false;
    }
    if (exceptionMessage != null ? !exceptionMessage.equals(that.exceptionMessage) : that.exceptionMessage != null) {
      return false;
    }
    return state == that.state;
  }

  @Override
  public int hashCode() {
    int result = exceptionMessage != null ? exceptionMessage.hashCode() : 0;
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (int) (waitTime ^ (waitTime >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "AgentCheckInResponse{" +
        "exceptionMessage=" + exceptionMessage +
        ", state=" + state +
        ", waitTime=" + waitTime +
        '}';
  }
}

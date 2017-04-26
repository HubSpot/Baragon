package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class AgentBatchResponseItem {
  private final String requestId;
  private final int statusCode;
  private final Optional<String> message;
  private final AgentRequestType requestType;

  @JsonCreator
  public AgentBatchResponseItem(@JsonProperty("requestId") String requestId,
                                @JsonProperty("statusCode") int statusCode,
                                @JsonProperty("message") Optional<String> message,
                                @JsonProperty("requestType") AgentRequestType requestType) {
    this.requestId = requestId;
    this.statusCode = statusCode;
    this.message = message;
    this.requestType = requestType;
  }

  public String getRequestId() {
    return requestId;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public AgentRequestType getRequestType() {
    return requestType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AgentBatchResponseItem that = (AgentBatchResponseItem) o;
    return statusCode == that.statusCode &&
      Objects.equal(requestId, that.requestId) &&
      Objects.equal(message, that.message) &&
      requestType == that.requestType;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, statusCode, message, requestType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("requestId", requestId)
      .add("statusCode", statusCode)
      .add("message", message)
      .add("requestType", requestType)
      .toString();
  }
}

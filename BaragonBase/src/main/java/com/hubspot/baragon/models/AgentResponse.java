package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class AgentResponse {
  private final Optional<Integer> statusCode;
  private final Optional<String> content;
  private final Optional<String> exception;

  @JsonCreator
  public AgentResponse(@JsonProperty("statusCode") Optional<Integer> statusCode,
                       @JsonProperty("content") Optional<String> content,
                       @JsonProperty("exception") Optional<String> exception) {
    this.statusCode = statusCode;
    this.content = content;
    this.exception = exception;
  }

  public Optional<Integer> getStatusCode() {
    return statusCode;
  }

  public Optional<String> getContent() {
    return content;
  }

  public Optional<String> getException() {
    return exception;
  }

  @JsonIgnore
  public AgentRequestStatus toRequestStatus() {
    if (!statusCode.isPresent() && !exception.isPresent()) {
      return AgentRequestStatus.WAITING;
    }

    if (statusCode.isPresent() && statusCode.get() >= 200 && statusCode.get() < 300) {
      return AgentRequestStatus.SUCCESS;
    }

    return AgentRequestStatus.FAILURE;
  }

  @JsonIgnore
  public boolean isSuccess() {
    return !exception.isPresent() && statusCode.isPresent() && statusCode.get() >= 200 & statusCode.get() < 300;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("statusCode", statusCode)
        .add("content", content)
        .add("exception", exception)
        .toString();
  }
}

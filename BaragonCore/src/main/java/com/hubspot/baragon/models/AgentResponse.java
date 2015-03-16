package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class AgentResponse {
  private final String url;
  private final int attempt;
  private final Optional<Integer> statusCode;
  private final Optional<String> content;
  private final Optional<String> exception;

  @JsonCreator
  public AgentResponse(@JsonProperty("url") String url,
                       @JsonProperty("attempt") int attempt,
                       @JsonProperty("statusCode") Optional<Integer> statusCode,
                       @JsonProperty("content") Optional<String> content,
                       @JsonProperty("exception") Optional<String> exception) {
    this.url = url;
    this.attempt = attempt;
    this.statusCode = statusCode;
    this.content = content;
    this.exception = exception;
  }

  public int getAttempt() {
    return attempt;
  }

  public String getUrl() {
    return url;
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
  public AgentRequestsStatus toRequestStatus() {
    if (!statusCode.isPresent() && !exception.isPresent()) {
      return AgentRequestsStatus.WAITING;
    }

    if (statusCode.isPresent() && statusCode.get() >= 200 && statusCode.get() < 300) {
      return AgentRequestsStatus.SUCCESS;
    }

    return AgentRequestsStatus.FAILURE;
  }

  @Override
  public String toString() {
    return "AgentResponse [" +
        "url='" + url + '\'' +
        ", attempt=" + attempt +
        ", statusCode=" + statusCode +
        ", content=" + content +
        ", exception=" + exception +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AgentResponse that = (AgentResponse) o;

    if (attempt != that.attempt) {
      return false;
    }
    if (!content.equals(that.content)) {
      return false;
    }
    if (!exception.equals(that.exception)) {
      return false;
    }
    if (!statusCode.equals(that.statusCode)) {
      return false;
    }
    if (!url.equals(that.url)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = url.hashCode();
    result = 31 * result + attempt;
    result = 31 * result + statusCode.hashCode();
    result = 31 * result + content.hashCode();
    result = 31 * result + exception.hashCode();
    return result;
  }
}

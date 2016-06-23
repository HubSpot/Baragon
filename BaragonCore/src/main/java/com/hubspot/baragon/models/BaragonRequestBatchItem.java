package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class BaragonRequestBatchItem {
  private final String requestId;
  private final Optional<RequestAction> requestAction;
  private final AgentRequestType requestType;
  private final Double priority;

  @JsonCreator
  public BaragonRequestBatchItem(@JsonProperty("requestId") String requestId,
                                 @JsonProperty("requestAction") Optional<RequestAction> requestAction,
                                 @JsonProperty("requestType") AgentRequestType requestType,
                                 @JsonProperty("priority") Double priority) {
    this.requestId = requestId;
    this.requestAction = requestAction;
    this.requestType = requestType;
    this.priority = priority;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<RequestAction> getRequestAction() {
    return requestAction;
  }

  public AgentRequestType getRequestType() {
    return requestType;
  }

  public Double getPriority() {
    return priority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaragonRequestBatchItem that = (BaragonRequestBatchItem) o;
    return Objects.equal(requestId, that.requestId) &&
      Objects.equal(requestAction, that.requestAction) &&
      requestType == that.requestType &&
      Objects.equal(priority, that.priority);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, requestAction, requestType, priority);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("requestId", requestId)
      .add("requestAction", requestAction)
      .add("requestType", requestType)
      .add("priority", priority)
      .toString();
  }
}

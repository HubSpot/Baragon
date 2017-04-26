package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class QueuedRequestWithState {
  private final QueuedRequestId queuedRequestId;
  private final BaragonRequest request;
  private final InternalRequestStates currentState;

  @JsonCreator
  public QueuedRequestWithState(@JsonProperty("queuedRequestId") QueuedRequestId queuedRequestId,
                                @JsonProperty("request") BaragonRequest request,
                                @JsonProperty("currentState") InternalRequestStates currentState) {
    this.queuedRequestId = queuedRequestId;
    this.request = request;
    this.currentState = currentState;
  }

  public QueuedRequestId getQueuedRequestId() {
    return queuedRequestId;
  }

  public BaragonRequest getRequest() {
    return request;
  }

  public InternalRequestStates getCurrentState() {
    return currentState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueuedRequestWithState that = (QueuedRequestWithState) o;
    return Objects.equal(queuedRequestId, that.queuedRequestId) &&
      Objects.equal(request, that.request) &&
      currentState == that.currentState;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(queuedRequestId, request, currentState);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("queuedRequestId", queuedRequestId)
      .add("request", request)
      .add("currentState", currentState)
      .toString();
  }
}

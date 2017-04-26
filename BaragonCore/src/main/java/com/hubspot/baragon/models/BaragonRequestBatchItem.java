package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class BaragonRequestBatchItem {
  private final String requestId;
  private final Optional<RequestAction> requestAction;
  private final AgentRequestType requestType;

  @JsonCreator
  public BaragonRequestBatchItem(@JsonProperty("requestId") String requestId,
                                 @JsonProperty("requestAction") Optional<RequestAction> requestAction,
                                 @JsonProperty("requestType") AgentRequestType requestType) {
    this.requestId = requestId;
    this.requestAction = requestAction;
    this.requestType = requestType;
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
      requestType == that.requestType;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, requestAction, requestType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("requestId", requestId)
      .add("requestAction", requestAction)
      .add("requestType", requestType)
      .toString();
  }
}

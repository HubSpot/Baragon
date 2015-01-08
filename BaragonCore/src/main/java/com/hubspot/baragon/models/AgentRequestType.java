package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgentRequestType {
  APPLY(InternalRequestStates.CHECK_APPLY_RESPONSES, InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, InternalRequestStates.SEND_APPLY_REQUESTS, InternalRequestStates.COMPLETED),
  REVERT(InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, InternalRequestStates.FAILED_REVERT_FAILED, InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, InternalRequestStates.FAILED_REVERTED),
  CANCEL(InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, InternalRequestStates.FAILED_CANCEL_FAILED, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, InternalRequestStates.CANCELLED);

  private final InternalRequestStates failureState;
  private final InternalRequestStates retryState;
  private final InternalRequestStates successState;
  private final InternalRequestStates waitingState;

  AgentRequestType(InternalRequestStates waitingState, InternalRequestStates failureState, InternalRequestStates retryState, InternalRequestStates successState) {
    this.waitingState = waitingState;
    this.failureState = failureState;
    this.retryState = retryState;
    this.successState = successState;
  }

  public InternalRequestStates getWaitingState() {
    return waitingState;
  }

  public InternalRequestStates getFailureState() {
    return failureState;
  }

  public InternalRequestStates getRetryState() {
    return retryState;
  }

  public InternalRequestStates getSuccessState() {
    return successState;
  }

  @JsonCreator
  public static AgentRequestType fromString(String stringValue) {
    for (AgentRequestType value : values()) {
      if (value.name().equalsIgnoreCase(stringValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown AgentRequestType: " + stringValue);
  }
}

package com.hubspot.baragon.models;

public enum InternalRequestStates {
  PENDING(false, false),
  INVALID_REQUEST_NOOP(false, false),
  SEND_APPLY_REQUESTS(true, false),
  CHECK_APPLY_RESPONSES(false, true),
  COMPLETED(false, false),
  FAILED_SEND_REVERT_REQUESTS(true, true),
  FAILED_CHECK_REVERT_RESPONSES(false, true),
  FAILED_REVERTED(false, false),
  FAILED_REVERT_FAILED(false, false),
  CANCELLED_SEND_REVERT_REQUESTS(true, true),
  CANCELLED_CHECK_REVERT_RESPONSES(false, true),
  CANCELLED(false, false),
  FAILED_CANCEL_FAILED(false, false),
  COMPLETED_POST_APPLY_FAILED(false, false);

  private final boolean requireAgentRequest;
  private final boolean inFlight;

  InternalRequestStates(boolean requireAgentRequest, boolean inFlight) {
    this.requireAgentRequest = requireAgentRequest;
    this.inFlight = inFlight;
  }

  public boolean isRequireAgentRequest() {
    return requireAgentRequest;
  }

  public boolean isInFlight() {
    return inFlight;
  }
}

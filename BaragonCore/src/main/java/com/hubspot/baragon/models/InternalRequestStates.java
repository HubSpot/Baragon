package com.hubspot.baragon.models;

public enum InternalRequestStates {
  PENDING(false),
  INVALID_REQUEST_NOOP(false),
  SEND_APPLY_REQUESTS(true),
  CHECK_APPLY_RESPONSES(false),
  COMPLETED(false),
  FAILED_SEND_REVERT_REQUESTS(true),
  FAILED_CHECK_REVERT_RESPONSES(false),
  FAILED_REVERTED(false),
  FAILED_REVERT_FAILED(false),
  CANCELLED_SEND_REVERT_REQUESTS(true),
  CANCELLED_CHECK_REVERT_RESPONSES(false),
  CANCELLED(false),
  FAILED_CANCEL_FAILED(false);

  private final boolean requireAgentRequest;

  InternalRequestStates(boolean requireAgentRequest) {
     this.requireAgentRequest = requireAgentRequest;
  }

  public boolean isRequireAgentRequest() {
    return requireAgentRequest;
  }
}

package com.hubspot.baragon.models;

public enum InternalRequestStates {
  PENDING(AgentRequestType.APPLY, BaragonRequestState.WAITING),
  INVALID_REQUEST_NOOP(AgentRequestType.APPLY, BaragonRequestState.INVALID_REQUEST_NOOP),
  SEND_APPLY_REQUESTS(AgentRequestType.APPLY, BaragonRequestState.WAITING),
  CHECK_APPLY_RESPONSES(AgentRequestType.APPLY, BaragonRequestState.WAITING),
  COMPLETED(AgentRequestType.APPLY, BaragonRequestState.SUCCESS),

  FAILED_SEND_REVERT_REQUESTS(AgentRequestType.REVERT, BaragonRequestState.WAITING),
  FAILED_CHECK_REVERT_RESPONSES(AgentRequestType.REVERT, BaragonRequestState.WAITING),
  FAILED_REVERTED(AgentRequestType.REVERT, BaragonRequestState.FAILED),
  FAILED_REVERT_FAILED(AgentRequestType.REVERT, BaragonRequestState.FAILED),

  CANCELLED_SEND_REVERT_REQUESTS(AgentRequestType.CANCEL, BaragonRequestState.CANCELING),
  CANCELLED_CHECK_REVERT_RESPONSES(AgentRequestType.CANCEL, BaragonRequestState.CANCELING),
  CANCELLED(AgentRequestType.CANCEL, BaragonRequestState.CANCELED),
  FAILED_CANCEL_FAILED(AgentRequestType.CANCEL, BaragonRequestState.FAILED);

  private final BaragonRequestState requestState;
  private final AgentRequestType requestType;

  private InternalRequestStates(AgentRequestType requestType, BaragonRequestState requestState) {
    this.requestType = requestType;
    this.requestState = requestState;
  }

  public AgentRequestType getRequestType() {
    return requestType;
  }

  public BaragonRequestState toRequestState() {
    return requestState;
  }

  public boolean isCancelable() {
    return requestState == BaragonRequestState.WAITING;
  }

  public boolean isRemovable() {
    return requestState == BaragonRequestState.SUCCESS || requestState == BaragonRequestState.FAILED || requestState == BaragonRequestState.CANCELED;
  }

}

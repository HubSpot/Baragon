package com.hubspot.baragon.models;

import com.google.common.collect.ImmutableMap;

public class InternalStatesMap {
  private static final ImmutableMap<InternalRequestStates, AgentRequestType> stateToRequestTypeMap = new ImmutableMap.Builder<InternalRequestStates, AgentRequestType>()
    .put(InternalRequestStates.PENDING, AgentRequestType.APPLY)
    .put(InternalRequestStates.INVALID_REQUEST_NOOP, AgentRequestType.APPLY)
    .put(InternalRequestStates.SEND_APPLY_REQUESTS, AgentRequestType.APPLY)
    .put(InternalRequestStates.CHECK_APPLY_RESPONSES, AgentRequestType.APPLY)
    .put(InternalRequestStates.COMPLETED, AgentRequestType.APPLY)
    .put(InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, AgentRequestType.REVERT)
    .put(InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, AgentRequestType.REVERT)
    .put(InternalRequestStates.FAILED_REVERT_FAILED, AgentRequestType.REVERT)
    .put(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, AgentRequestType.CANCEL)
    .put(InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, AgentRequestType.CANCEL)
    .put(InternalRequestStates.CANCELLED, AgentRequestType.CANCEL)
    .put(InternalRequestStates.FAILED_CANCEL_FAILED, AgentRequestType.CANCEL)
    .build();

  private static final ImmutableMap<InternalRequestStates, BaragonRequestState> stateToRequestStateMap = new ImmutableMap.Builder<InternalRequestStates, BaragonRequestState>()
    .put(InternalRequestStates.PENDING, BaragonRequestState.WAITING)
    .put(InternalRequestStates.INVALID_REQUEST_NOOP, BaragonRequestState.INVALID_REQUEST_NOOP)
    .put(InternalRequestStates.SEND_APPLY_REQUESTS, BaragonRequestState.WAITING)
    .put(InternalRequestStates.CHECK_APPLY_RESPONSES, BaragonRequestState.WAITING)
    .put(InternalRequestStates.COMPLETED, BaragonRequestState.SUCCESS)
    .put(InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, BaragonRequestState.WAITING)
    .put(InternalRequestStates.FAILED_REVERTED, BaragonRequestState.FAILED)
    .put(InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, BaragonRequestState.WAITING)
    .put(InternalRequestStates.FAILED_REVERT_FAILED, BaragonRequestState.FAILED)
    .put(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, BaragonRequestState.CANCELING)
    .put(InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, BaragonRequestState.CANCELING)
    .put(InternalRequestStates.CANCELLED, BaragonRequestState.CANCELED)
    .put(InternalRequestStates.FAILED_CANCEL_FAILED, BaragonRequestState.FAILED)
    .build();

  private static final ImmutableMap<AgentRequestType, ImmutableMap<String, InternalRequestStates>> typeToRequestStateMap = new ImmutableMap.Builder<AgentRequestType, ImmutableMap<String, InternalRequestStates>>()
    .put(AgentRequestType.APPLY, ImmutableMap.of("waiting", InternalRequestStates.CHECK_APPLY_RESPONSES, "failure", InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, "retry", InternalRequestStates.SEND_APPLY_REQUESTS, "success", InternalRequestStates.COMPLETED))
    .put(AgentRequestType.REVERT, ImmutableMap.of("waiting", InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, "failure", InternalRequestStates.FAILED_REVERT_FAILED, "retry", InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, "success", InternalRequestStates.FAILED_REVERTED))
    .put(AgentRequestType.CANCEL, ImmutableMap.of("waiting", InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, "failure", InternalRequestStates.FAILED_CANCEL_FAILED, "retry", InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, "success", InternalRequestStates.CANCELLED))
    .build();

  public static InternalRequestStates getWaitingState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get("waiting");
  }

  public static InternalRequestStates getFailureState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get("failure");
  }

  public static InternalRequestStates getRetryState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get("retry");
  }

  public static InternalRequestStates getSuccessState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get("success");
  }

  public static AgentRequestType getRequestType(InternalRequestStates requestState) {
    return stateToRequestTypeMap.get(requestState);
  }

  public static BaragonRequestState getRequestState(InternalRequestStates requestState) {
    return stateToRequestStateMap.get(requestState);
  }

  public static boolean isCancelable(InternalRequestStates requestState) {
    return stateToRequestStateMap.get(requestState) == BaragonRequestState.WAITING;
  }

  public static boolean isRemovable(InternalRequestStates requestState) {
    BaragonRequestState state = stateToRequestStateMap.get(requestState);
    return state == BaragonRequestState.SUCCESS || state == BaragonRequestState.FAILED || state == BaragonRequestState.CANCELED;
  }

}

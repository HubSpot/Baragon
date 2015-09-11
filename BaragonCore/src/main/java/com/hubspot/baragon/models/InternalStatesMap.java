package com.hubspot.baragon.models;

import java.util.EnumMap;

public class InternalStatesMap {
  private static EnumMap<InternalRequestStates, AgentRequestType> stateToRequestTypeMap = new EnumMap<>(InternalRequestStates.class);
  static {
    stateToRequestTypeMap.put(InternalRequestStates.PENDING, AgentRequestType.APPLY);
    stateToRequestTypeMap.put(InternalRequestStates.INVALID_REQUEST_NOOP, AgentRequestType.APPLY);
    stateToRequestTypeMap.put(InternalRequestStates.SEND_APPLY_REQUESTS, AgentRequestType.APPLY);
    stateToRequestTypeMap.put(InternalRequestStates.CHECK_APPLY_RESPONSES, AgentRequestType.APPLY);
    stateToRequestTypeMap.put(InternalRequestStates.COMPLETED, AgentRequestType.APPLY);
    stateToRequestTypeMap.put(InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, AgentRequestType.REVERT);
    stateToRequestTypeMap.put(InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, AgentRequestType.REVERT);
    stateToRequestTypeMap.put(InternalRequestStates.FAILED_REVERT_FAILED, AgentRequestType.REVERT);
    stateToRequestTypeMap.put(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, AgentRequestType.CANCEL);
    stateToRequestTypeMap.put(InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, AgentRequestType.CANCEL);
    stateToRequestTypeMap.put(InternalRequestStates.CANCELLED, AgentRequestType.CANCEL);
    stateToRequestTypeMap.put(InternalRequestStates.FAILED_CANCEL_FAILED, AgentRequestType.CANCEL);
  }

  private static EnumMap<InternalRequestStates, BaragonRequestState> stateToRequestStateMap = new EnumMap<>(InternalRequestStates.class);
  static {
    stateToRequestStateMap.put(InternalRequestStates.PENDING, BaragonRequestState.WAITING);
    stateToRequestStateMap.put(InternalRequestStates.INVALID_REQUEST_NOOP, BaragonRequestState.INVALID_REQUEST_NOOP);
    stateToRequestStateMap.put(InternalRequestStates.SEND_APPLY_REQUESTS, BaragonRequestState.WAITING);
    stateToRequestStateMap.put(InternalRequestStates.CHECK_APPLY_RESPONSES, BaragonRequestState.WAITING);
    stateToRequestStateMap.put(InternalRequestStates.COMPLETED, BaragonRequestState.SUCCESS);
    stateToRequestStateMap.put(InternalRequestStates.FAILED_SEND_REVERT_REQUESTS, BaragonRequestState.WAITING);
    stateToRequestStateMap.put(InternalRequestStates.FAILED_REVERTED, BaragonRequestState.FAILED);
    stateToRequestStateMap.put(InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES, BaragonRequestState.WAITING);
    stateToRequestStateMap.put(InternalRequestStates.FAILED_REVERT_FAILED, BaragonRequestState.FAILED);
    stateToRequestStateMap.put(InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS, BaragonRequestState.CANCELING);
    stateToRequestStateMap.put(InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES, BaragonRequestState.CANCELING);
    stateToRequestStateMap.put(InternalRequestStates.CANCELLED, BaragonRequestState.CANCELED);
    stateToRequestStateMap.put(InternalRequestStates.FAILED_CANCEL_FAILED, BaragonRequestState.FAILED);
  }

  private static EnumMap<AgentRequestType, EnumMap<AgentRequestsStatus, InternalRequestStates>> typeToRequestStateMap = new EnumMap<>(AgentRequestType.class);
  static {
    EnumMap<AgentRequestsStatus, InternalRequestStates> applyMap = new EnumMap<>(AgentRequestsStatus.class);
    applyMap.put(AgentRequestsStatus.WAITING, InternalRequestStates.CHECK_APPLY_RESPONSES);
    applyMap.put(AgentRequestsStatus.FAILURE, InternalRequestStates.FAILED_SEND_REVERT_REQUESTS);
    applyMap.put(AgentRequestsStatus.RETRY, InternalRequestStates.SEND_APPLY_REQUESTS);
    applyMap.put(AgentRequestsStatus.SUCCESS, InternalRequestStates.COMPLETED);
    typeToRequestStateMap.put(AgentRequestType.APPLY, applyMap);

    EnumMap<AgentRequestsStatus, InternalRequestStates> revertMap = new EnumMap<>(AgentRequestsStatus.class);
    revertMap.put(AgentRequestsStatus.WAITING, InternalRequestStates.FAILED_CHECK_REVERT_RESPONSES);
    revertMap.put(AgentRequestsStatus.FAILURE, InternalRequestStates.FAILED_REVERT_FAILED);
    revertMap.put(AgentRequestsStatus.RETRY, InternalRequestStates.FAILED_SEND_REVERT_REQUESTS);
    revertMap.put(AgentRequestsStatus.SUCCESS, InternalRequestStates.FAILED_REVERTED);;
    typeToRequestStateMap.put(AgentRequestType.REVERT, revertMap);

    EnumMap<AgentRequestsStatus, InternalRequestStates> cancelMap = new EnumMap<>(AgentRequestsStatus.class);
    cancelMap.put(AgentRequestsStatus.WAITING, InternalRequestStates.CANCELLED_CHECK_REVERT_RESPONSES);
    cancelMap.put(AgentRequestsStatus.FAILURE, InternalRequestStates.FAILED_CANCEL_FAILED);
    cancelMap.put(AgentRequestsStatus.RETRY, InternalRequestStates.CANCELLED_SEND_REVERT_REQUESTS);
    cancelMap.put(AgentRequestsStatus.SUCCESS, InternalRequestStates.CANCELLED);
    typeToRequestStateMap.put(AgentRequestType.CANCEL, cancelMap);
  }

  public static InternalRequestStates getWaitingState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get(AgentRequestsStatus.WAITING);
  }

  public static InternalRequestStates getFailureState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get(AgentRequestsStatus.FAILURE);
  }

  public static InternalRequestStates getRetryState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get(AgentRequestsStatus.RETRY);
  }

  public static InternalRequestStates getSuccessState(InternalRequestStates requestState) {
    return typeToRequestStateMap.get(stateToRequestTypeMap.get(requestState)).get(AgentRequestsStatus.SUCCESS);
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
    return state == BaragonRequestState.SUCCESS || state == BaragonRequestState.FAILED || state == BaragonRequestState.CANCELED || state == BaragonRequestState.INVALID_REQUEST_NOOP;
  }

}

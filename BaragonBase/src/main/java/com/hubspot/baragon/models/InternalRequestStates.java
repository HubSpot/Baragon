package com.hubspot.baragon.models;

import com.google.common.base.Optional;
import com.hubspot.baragon.managers.AgentManager;
import com.hubspot.baragon.managers.RequestManager;

public enum InternalRequestStates {
  SEND_APPLY_REQUESTS(RequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.APPLY);

      return Optional.of(CHECK_APPLY_RESPONSES);
    }
  },

  CHECK_APPLY_RESPONSES(RequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.APPLY)) {
        case FAILURE:
          return Optional.of(FAILED_SEND_REVERT_REQUESTS);
        case SUCCESS:
          requestManager.commitRequest(request);
          return Optional.of(COMPLETED);
        case RETRY:
          return Optional.of(SEND_APPLY_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  COMPLETED(RequestState.SUCCESS, false, true),

  FAILED_SEND_REVERT_REQUESTS(RequestState.FAILED, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.REVERT);

      return Optional.of(FAILED_CHECK_REVERT_RESPONSES);
    }
  },

  FAILED_CHECK_REVERT_RESPONSES(RequestState.FAILED, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.REVERT)) {
        case FAILURE:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Operation failed, revert failed!");
          return Optional.of(FAILED_REVERT_FAILED);
        case SUCCESS:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Operation failed, revert succeeded!");
          return Optional.of(FAILED_REVERTED);
        case RETRY:
          return Optional.of(FAILED_SEND_REVERT_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  FAILED_REVERTED(RequestState.FAILED, false, true),
  FAILED_REVERT_FAILED(RequestState.FAILED, false, true),

  CANCELLED_SEND_REVERT_REQUESTS(RequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.REVERT);

      return Optional.of(CANCELLED_CHECK_REVERT_RESPONSES);
    }
  },

  CANCELLED_CHECK_REVERT_RESPONSES(RequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.REVERT)) {
        case FAILURE:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Cancel operation failed");
          return Optional.of(FAILED_CANCEL_FAILED);
        case SUCCESS:
          return Optional.of(CANCELLED);
        case RETRY:
          return Optional.of(CANCELLED_SEND_REVERT_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  FAILED_CANCEL_FAILED(RequestState.FAILED, false, true),
  CANCELLED(RequestState.CANCELED, false, true);

  private final boolean cancelable;
  private final boolean removable;
  private final RequestState requestState;

  private InternalRequestStates(RequestState requestState, boolean cancelable, boolean removable) {
    this.requestState = requestState;
    this.cancelable = cancelable;
    this.removable = removable;
  }

  public RequestState toRequestState() {
    return requestState;
  }

  public boolean isCancelable() {
    return cancelable;
  }

  public boolean isRemovable() {
    return removable;
  }

  public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
    return Optional.absent();
  }
}

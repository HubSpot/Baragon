package com.hubspot.baragon.models;

import com.google.common.base.Optional;
import com.hubspot.baragon.managers.AgentManager;
import com.hubspot.baragon.managers.RequestManager;

public enum InternalRequestStates {
  SEND_APPLY_REQUESTS(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.APPLY);

      return Optional.of(CHECK_APPLY_RESPONSES);
    }
  },

  CHECK_APPLY_RESPONSES(BaragonRequestState.WAITING, true, false) {
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

  COMPLETED(BaragonRequestState.SUCCESS, false, true),

  FAILED_SEND_REVERT_REQUESTS(BaragonRequestState.FAILED, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.REVERT);

      return Optional.of(FAILED_CHECK_REVERT_RESPONSES);
    }
  },

  FAILED_CHECK_REVERT_RESPONSES(BaragonRequestState.FAILED, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.REVERT)) {
        case FAILURE:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Apply failed, revert failed!");
          return Optional.of(FAILED_REVERT_FAILED);
        case SUCCESS:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Apply failed, revert succeeded!");
          return Optional.of(FAILED_REVERTED);
        case RETRY:
          return Optional.of(FAILED_SEND_REVERT_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  FAILED_REVERTED(BaragonRequestState.FAILED, false, true),
  FAILED_REVERT_FAILED(BaragonRequestState.FAILED, false, true),

  CANCELLED_SEND_REVERT_REQUESTS(BaragonRequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.CANCEL);

      return Optional.of(CANCELLED_CHECK_REVERT_RESPONSES);
    }
  },

  CANCELLED_CHECK_REVERT_RESPONSES(BaragonRequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.CANCEL)) {
        case FAILURE:
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), "Cancel failed!");
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

  FAILED_CANCEL_FAILED(BaragonRequestState.FAILED, false, true),
  CANCELLED(BaragonRequestState.CANCELED, false, true);

  private final boolean cancelable;
  private final boolean removable;
  private final BaragonRequestState requestState;

  private InternalRequestStates(BaragonRequestState requestState, boolean cancelable, boolean removable) {
    this.requestState = requestState;
    this.cancelable = cancelable;
    this.removable = removable;
  }

  public BaragonRequestState toRequestState() {
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

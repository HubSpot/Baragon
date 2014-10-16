package com.hubspot.baragon.models;

public enum BaragonRequestState {
  UNKNOWN(false), FAILED(false), WAITING(true), SUCCESS(false), CANCELING(true), CANCELED(false), INVALID_REQUEST_NOOP(false);

  private final boolean inProgress;

  BaragonRequestState(boolean inProgress) {
    this.inProgress = inProgress;
  }

  public boolean isInProgress() {
    return inProgress;
  }
}

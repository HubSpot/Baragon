package com.hubspot.baragon.service.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class ServiceStatus {
  private final boolean leader;
  private final int pendingRequests;
  private final long workerLagMs;

  @JsonCreator
  public ServiceStatus(@JsonProperty("leader") boolean leader,
                       @JsonProperty("pendingRequests") int pendingRequests,
                       @JsonProperty("workerLagMs") long workerLagMs) {
    this.leader = leader;
    this.pendingRequests = pendingRequests;
    this.workerLagMs = workerLagMs;
  }

  public boolean isLeader() {
    return leader;
  }

  public int getPendingRequests() {
    return pendingRequests;
  }

  public long getWorkerLagMs() {
    return workerLagMs;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("leader", leader)
        .add("pendingRequests", pendingRequests)
        .add("workerLagMs", workerLagMs)
        .toString();
  }
}

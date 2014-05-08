package com.hubspot.baragon.service.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class ServiceStatus {
  private final boolean leader;
  private final int pendingRequestCount;
  private final long workerLagMs;

  @JsonCreator
  public ServiceStatus(@JsonProperty("leader") boolean leader,
                       @JsonProperty("pendingRequestCount") int pendingRequestCount,
                       @JsonProperty("workerLagMs") long workerLagMs) {
    this.leader = leader;
    this.pendingRequestCount = pendingRequestCount;
    this.workerLagMs = workerLagMs;
  }

  public boolean isLeader() {
    return leader;
  }

  public int getPendingRequestCount() {
    return pendingRequestCount;
  }

  public long getWorkerLagMs() {
    return workerLagMs;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("leader", leader)
        .add("pendingRequestCount", pendingRequestCount)
        .add("workerLagMs", workerLagMs)
        .toString();
  }
}

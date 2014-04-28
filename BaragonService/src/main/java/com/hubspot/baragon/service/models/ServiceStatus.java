package com.hubspot.baragon.service.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class ServiceStatus {
  private final boolean leader;
  private final int pendingRequests;

  @JsonCreator
  public ServiceStatus(@JsonProperty("leader") boolean leader,
                       @JsonProperty("pendingRequests") int pendingRequests) {
    this.leader = leader;
    this.pendingRequests = pendingRequests;
  }

  public boolean isLeader() {
    return leader;
  }

  public int getPendingRequests() {
    return pendingRequests;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("leader", leader)
        .add("pendingRequests", pendingRequests)
        .toString();
  }
}

package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BaragonServiceStatus {
  private final boolean leader;
  private final int pendingRequestCount;
  private final long workerLagMs;

  @JsonCreator
  public BaragonServiceStatus(@JsonProperty("leader") boolean leader,
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonServiceStatus that = (BaragonServiceStatus) o;

    if (leader != that.leader) return false;
    if (pendingRequestCount != that.pendingRequestCount) return false;
    if (workerLagMs != that.workerLagMs) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (leader ? 1 : 0);
    result = 31 * result + pendingRequestCount;
    result = 31 * result + (int) (workerLagMs ^ (workerLagMs >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "BaragonServiceStatus [" +
        "leader=" + leader +
        ", pendingRequestCount=" + pendingRequestCount +
        ", workerLagMs=" + workerLagMs +
        ']';
  }
}

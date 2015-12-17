package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonServiceStatus {
  private final boolean leader;
  private final int pendingRequestCount;
  private final long workerLagMs;
  private final long elbWorkerLagMs;
  private final String zookeeperState;

  @JsonCreator
  public BaragonServiceStatus(@JsonProperty("leader") boolean leader,
                              @JsonProperty("pendingRequestCount") int pendingRequestCount,
                              @JsonProperty("workerLagMs") long workerLagMs,
                              @JsonProperty("elbWorkerLagMs") long elbWorkerLagMs,
                              @JsonProperty("zookeeperState") String zookeeperState) {
    this.leader = leader;
    this.pendingRequestCount = pendingRequestCount;
    this.workerLagMs = workerLagMs;
    this.elbWorkerLagMs = elbWorkerLagMs;
    this.zookeeperState = zookeeperState;
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

  public long getElbWorkerLagMs() {
    return elbWorkerLagMs;
  }

  public String getZookeeperState() {
    return zookeeperState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonServiceStatus that = (BaragonServiceStatus) o;

    if (leader != that.leader) {
      return false;
    }
    if (pendingRequestCount != that.pendingRequestCount) {
      return false;
    }
    if (workerLagMs != that.workerLagMs) {
      return false;
    }
    if (elbWorkerLagMs != that.elbWorkerLagMs) {
      return false;
    }
    if (!zookeeperState.equals(that.zookeeperState)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = (leader ? 1 : 0);
    result = 31 * result + pendingRequestCount;
    result = 31 * result + (int) (workerLagMs ^ (workerLagMs >>> 32));
    result = 31 * result + (int) (elbWorkerLagMs ^ (elbWorkerLagMs >>> 32));
    result = 31 * result + zookeeperState.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BaragonServiceStatus [" +
        "leader=" + leader +
        ", pendingRequestCount=" + pendingRequestCount +
        ", workerLagMs=" + workerLagMs +
        ",  elbWorkerLagMs=" + elbWorkerLagMs +
        ", zookeeperState='" + zookeeperState + '\'' +
        ']';
  }
}

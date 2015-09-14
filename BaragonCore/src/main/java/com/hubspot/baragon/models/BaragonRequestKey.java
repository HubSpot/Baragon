package com.hubspot.baragon.models;

public class BaragonRequestKey implements Comparable<BaragonRequestKey> {
  private final String requestId;
  private final long updatedAt;

  public BaragonRequestKey(String requestId, long updatedAt) {
    this.requestId = requestId;
    this.updatedAt = updatedAt;
  }

  public String getRequestId() {
    return requestId;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public int compareTo(BaragonRequestKey o) {
    return Long.compare(updatedAt, o.updatedAt);
  }
}

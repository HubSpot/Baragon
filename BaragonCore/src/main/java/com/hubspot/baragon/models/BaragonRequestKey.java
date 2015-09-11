package com.hubspot.baragon.models;

public class BaragonRequestKey implements Comparable<BaragonRequestKey> {
  private final String requestId;
  private final long updatedAt;

  public BaragonRequestKey(String reqeustId, long updatedAt) {
    this.requestId = reqeustId;
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
    return updatedAt >= o.updatedAt ? -1 : 1;
  }
}

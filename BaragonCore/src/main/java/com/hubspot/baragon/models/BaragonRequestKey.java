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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonRequestKey that = (BaragonRequestKey) o;

    if (updatedAt != that.updatedAt) {
      return false;
    }
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = requestId != null ? requestId.hashCode() : 0;
    result = 31 * result + (int) (updatedAt ^ (updatedAt >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "BaragonRequestKey{" +
      "requestId='" + requestId + '\'' +
      ", updatedAt=" + updatedAt +
      '}';
  }
}

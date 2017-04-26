package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class QueuedRequestId {
  private final String serviceId;
  private final String requestId;
  private final int index;

  public static QueuedRequestId fromString(String value) {
    final String[] splits = value.split("\\|", 3);

    return new QueuedRequestId(splits[0], splits[1], Integer.parseInt(splits[2]));
  }

  @JsonCreator
  public QueuedRequestId(@JsonProperty("serviceId") String serviceId,
                         @JsonProperty("requestId") String requestId,
                         @JsonProperty("index") int index) {
    this.serviceId = serviceId;
    this.requestId = requestId;
    this.index = index;
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getRequestId() {
    return requestId;
  }

  public int getIndex() {
    return index;
  }

  @JsonIgnore
  public String buildZkPath() {
    return String.format("%s|%s|%010d", serviceId, requestId, index);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueuedRequestId that = (QueuedRequestId) o;

    if (index != that.index) {
      return false;
    }
    if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null) {
      return false;
    }
    return requestId != null ? requestId.equals(that.requestId) : that.requestId == null;

  }

  @Override
  public int hashCode() {
    int result = serviceId != null ? serviceId.hashCode() : 0;
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + index;
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serviceId", serviceId)
        .add("requestId", requestId)
        .add("index", index)
        .toString();
  }
}

package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

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
  public String toString() {
    return Objects.toStringHelper(this)
        .add("serviceId", serviceId)
        .add("requestId", requestId)
        .add("index", index)
        .toString();
  }
}

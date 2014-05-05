package com.hubspot.baragon.models;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;

public class QueuedRequestId {
  private static final Splitter DASH_SPLITTER = Splitter.on('_');

  private final String serviceId;
  private final String requestId;
  private final int index;

  public static QueuedRequestId fromString(String str) {
    final List<String> splits = Lists.newArrayList(DASH_SPLITTER.split(str));

    return new QueuedRequestId(splits.get(0), splits.get(1), Integer.parseInt(splits.get(2)));
  }

  private QueuedRequestId(String serviceId, String requestId, int index) {
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

  @Override
  public String toString() {
    return String.format("%s_%s_%010d", serviceId, requestId, index);
  }
}

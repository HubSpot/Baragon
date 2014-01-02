package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

public class ServiceSnapshot {
  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy");
  private final ServiceInfo serviceInfo;
  private final Collection<String> healthyUpstreams;
  private final Collection<String> unhealthyUpstreams;
  private final Collection<String> disabledUpstreams;
  private final Long timestamp;
  private final String timestampHuman;

  @JsonCreator
  public ServiceSnapshot(@JsonProperty("serviceInfo") ServiceInfo serviceInfo,
                         @JsonProperty("healthyUpstreams") Collection<String> healthyUpstreams,
                         @JsonProperty("unhealthyUpstreams") Collection<String> unhealthyUpstreams,
                         @JsonProperty("disabledUpstreams") Collection<String> disabledUpstreams,
                         @JsonProperty("timestamp") Long timestamp) {
    this.serviceInfo = serviceInfo;
    this.healthyUpstreams = healthyUpstreams;
    this.unhealthyUpstreams = unhealthyUpstreams;
    this.disabledUpstreams = disabledUpstreams;
    this.timestamp = timestamp;

    // generate human readable timestamp
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(timestamp);
    timestampHuman = DATE_FORMATTER.format(cal.getTime());
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public Collection<String> getHealthyUpstreams() {
    return healthyUpstreams;
  }

  public Collection<String> getUnhealthyUpstreams() {
    return unhealthyUpstreams;
  }

  public Collection<String> getDisabledUpstreams() {
    return disabledUpstreams;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  @JsonIgnore
  public String getTimestampHuman() {
    return timestampHuman;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceInfo, healthyUpstreams, unhealthyUpstreams, disabledUpstreams, timestamp);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(ServiceSnapshot.class)
        .add("serviceInfo", serviceInfo)
        .add("healthyUpstreams", healthyUpstreams)
        .add("unhealthyUpstreams", unhealthyUpstreams)
        .add("disabledUpstreams", disabledUpstreams)
        .add("timestamp", timestamp)
        .toString();
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof ServiceSnapshot) {
      return Objects.equal(this.serviceInfo, ((ServiceSnapshot)that).getServiceInfo())
          && Objects.equal(this.healthyUpstreams, ((ServiceSnapshot)that).getHealthyUpstreams())
          && Objects.equal(this.unhealthyUpstreams, ((ServiceSnapshot)that).getUnhealthyUpstreams())
          && Objects.equal(this.disabledUpstreams, ((ServiceSnapshot)that).getDisabledUpstreams())
          && Objects.equal(this.timestamp, ((ServiceSnapshot)that).getTimestamp());
    }

    return false;
  }
}

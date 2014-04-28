package com.hubspot.baragon.agent.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hubspot.baragon.models.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceContext {
  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

  private final Service service;
  private final Collection<String> upstreams;
  private final Long timestamp;
  private final String timestampHuman;

  @JsonCreator
  public ServiceContext(@JsonProperty("service") Service service,
                        @JsonProperty("upstreams") Collection<String> upstreams,
                        @JsonProperty("timestamp") Long timestamp) {
    this.service = service;
    this.timestamp = timestamp;

    // protect against NPEs
    if (upstreams != null) {
      this.upstreams = upstreams;
    } else {
      this.upstreams = Collections.emptyList();
    }

    // generate human readable timestamp
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(timestamp);
    timestampHuman = DATE_FORMATTER.format(cal.getTime());
  }

  public Service getService() {
    return service;
  }

  public Collection<String> getUpstreams() {
    return upstreams;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  @JsonIgnore
  public String getTimestampHuman() {
    return timestampHuman;
  }

  @JsonIgnore
  public boolean getHasUpstreams() { return !upstreams.isEmpty(); }

  @Override
  public int hashCode() {
    return Objects.hashCode(service, upstreams, timestamp);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(ServiceContext.class)
        .add("service", service)
        .add("upstreams", upstreams)
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

    if (that instanceof ServiceContext) {
      return Objects.equal(this.service, ((ServiceContext)that).getService())
          && Objects.equal(this.upstreams, ((ServiceContext)that).getUpstreams())
          && Objects.equal(this.timestamp, ((ServiceContext)that).getTimestamp());
    }

    return false;
  }
}

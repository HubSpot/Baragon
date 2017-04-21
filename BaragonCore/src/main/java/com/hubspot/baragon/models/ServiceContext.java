package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceContext {
  private final BaragonService service;
  private final Collection<UpstreamInfo> upstreams;
  private final Map<String, Collection<UpstreamInfo>> upstreamGroups;
  private final Long timestamp;
  private final boolean present;
  private final boolean rootPath;

  @JsonCreator
  public ServiceContext(@JsonProperty("service") BaragonService service,
                        @JsonProperty("upstreams") Collection<UpstreamInfo> upstreams,
                        @JsonProperty("timestamp") Long timestamp,
                        @JsonProperty("present") boolean present) {
    this.service = service;
    this.timestamp = timestamp;
    this.upstreams = MoreObjects.firstNonNull(upstreams, Collections.<UpstreamInfo>emptyList());
    this.present = present;
    this.rootPath = service.getServiceBasePath().equals("/");

    if (!this.upstreams.isEmpty()) {
      final Multimap<String, UpstreamInfo> upstreamGroupsMultimap = ArrayListMultimap.create();
      for (UpstreamInfo upstream : this.upstreams) {
        upstreamGroupsMultimap.put(upstream.getGroup(), upstream);
      }
      this.upstreamGroups = upstreamGroupsMultimap.asMap();
    } else {
      this.upstreamGroups = Collections.emptyMap();
    }
  }

  public BaragonService getService() {
    return service;
  }

  public Collection<UpstreamInfo> getUpstreams() {
    return upstreams;
  }

  public Map<String, Collection<UpstreamInfo>> getUpstreamGroups() {
    return upstreamGroups;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public boolean isPresent() {
    return present;
  }

  public boolean isRootPath() {
    return rootPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceContext that = (ServiceContext) o;

    if (present != that.present) {
      return false;
    }
    if (!service.equals(that.service)) {
      return false;
    }
    if (!timestamp.equals(that.timestamp)) {
      return false;
    }
    if (!upstreams.equals(that.upstreams)) {
      return false;
    }
    if (rootPath != that.rootPath) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = service.hashCode();
    result = 31 * result + upstreams.hashCode();
    result = 31 * result + timestamp.hashCode();
    result = 31 * result + (present ? 1 : 0);
    result = 31 * result + (rootPath ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ServiceContext [" +
        "service=" + service +
        ", upstreams=" + upstreams +
        ", timestamp=" + timestamp +
        ", present=" + present +
        ", rootPath=" + rootPath +
        ']';
  }
}

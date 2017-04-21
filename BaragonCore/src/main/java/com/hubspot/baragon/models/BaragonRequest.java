package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonRequest {
  @NotNull
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'", flags = Pattern.Flag.MULTILINE)
  private final String loadBalancerRequestId;

  @NotNull
  @Valid
  private final BaragonService loadBalancerService;

  @NotNull
  @Valid
  private final List<UpstreamInfo> addUpstreams;

  @NotNull
  @Valid
  private final List<UpstreamInfo> removeUpstreams;

  private final Optional<String> replaceServiceId;

  private final Optional<RequestAction> action;

  @Valid
  private final List<UpstreamInfo> replaceUpstreams;

  @NotNull
  private final boolean noValidate;

  @NotNull
  private final boolean noReload;

  @JsonCreator
  public BaragonRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                        @JsonProperty("loadBalancerService") BaragonService loadBalancerService,
                        @JsonProperty("addUpstreams") List<UpstreamInfo> addUpstreams,
                        @JsonProperty("removeUpstreams") List<UpstreamInfo> removeUpstreams,
                        @JsonProperty("replaceUpstreams") List<UpstreamInfo> replaceUpstreams,
                        @JsonProperty("replaceServiceId") Optional<String> replaceServiceId,
                        @JsonProperty("action") Optional<RequestAction> action,
                        @JsonProperty("noValidate") boolean noValidate,
                        @JsonProperty("noReload") boolean noReload) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = addRequestId(addUpstreams, loadBalancerRequestId);
    this.removeUpstreams = addRequestId(removeUpstreams, loadBalancerRequestId);
    this.replaceServiceId = replaceServiceId;
    this.action = action;
    this.replaceUpstreams = MoreObjects.firstNonNull(replaceUpstreams, Collections.<UpstreamInfo>emptyList());
    this.noValidate = MoreObjects.firstNonNull(noValidate, false);
    this.noReload = noReload;

  }

  public BaragonRequest(String loadBalancerRequestId, BaragonService loadBalancerService, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams, List<UpstreamInfo> replaceUpstreams, Optional<String> replaceServiceId, Optional<RequestAction> action) {
    this(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, replaceUpstreams, replaceServiceId, action, false, false);
  }

  public BaragonRequest(String loadBalancerRequestId, BaragonService loadBalancerService, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams) {
    this(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, Collections.<UpstreamInfo>emptyList(),Optional.<String>absent(), Optional.of(RequestAction.UPDATE), false, false);
  }

  public BaragonRequest(String loadBalancerRequestId, BaragonService loadBalancerService, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams, Optional<String> replaceServiceId) {
    this(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, Collections.<UpstreamInfo>emptyList(), replaceServiceId, Optional.of(RequestAction.UPDATE), false, false);
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public BaragonService getLoadBalancerService() {
    return loadBalancerService;
  }

  public List<UpstreamInfo> getAddUpstreams() {
    return addUpstreams;
  }

  public List<UpstreamInfo> getRemoveUpstreams() {
    return removeUpstreams;
  }

  public List<UpstreamInfo> getReplaceUpstreams() {
    return replaceUpstreams;
  }

  public Optional<String> getReplaceServiceId() {
    return replaceServiceId;
  }

  public Optional<RequestAction> getAction() {
    return action;
  }

  private List<UpstreamInfo> addRequestId(List<UpstreamInfo> upstreams, String requestId) {
    if (upstreams == null || requestId == null) {
      return upstreams;
    }

    List<UpstreamInfo> upstreamsWithRequestId = Lists.newArrayListWithCapacity(upstreams.size());
    for (UpstreamInfo upstream : upstreams) {
      upstreamsWithRequestId.add(addRequestId(upstream, requestId));
    }

    return upstreamsWithRequestId;
  }

  private UpstreamInfo addRequestId(UpstreamInfo upstream, String requestId) {
    if (!upstream.getRequestId().isPresent()) {
      return new UpstreamInfo(upstream.getUpstream(), Optional.of(requestId), upstream.getRackId(), Optional.of(upstream.getGroup()));
    } else {
      return upstream;
    }
  }

  public boolean isNoValidate() {
    return noValidate;
  }

  public boolean isNoReload() {
    return noReload;
  }

  @Override
  public String toString() {
    return "BaragonRequest [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerService=" + loadBalancerService +
        ", addUpstreams=" + addUpstreams +
        ", removeUpstreams=" + removeUpstreams +
        ", replaceServiceId=" + replaceServiceId +
        ", action=" + action +
        ", noValidate=" + noValidate +
        ", noReload=" + noReload +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonRequest request = (BaragonRequest) o;

    if (!addUpstreams.equals(request.addUpstreams)) {
      return false;
    }
    if (!loadBalancerRequestId.equals(request.loadBalancerRequestId)) {
      return false;
    }
    if (!loadBalancerService.equals(request.loadBalancerService)) {
      return false;
    }
    if (!removeUpstreams.equals(request.removeUpstreams)) {
      return false;
    }
    if (!replaceServiceId.equals(request.replaceServiceId)) {
      return false;
    }
    if (!action.equals(request.getAction())) {
      return false;
    }
    if (!noValidate == request.noValidate) {
      return false;
    }
    if (!noReload == request.noReload) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = loadBalancerRequestId.hashCode();
    result = 31 * result + loadBalancerService.hashCode();
    result = 31 * result + addUpstreams.hashCode();
    result = 31 * result + removeUpstreams.hashCode();
    result = 31 * result + replaceServiceId.hashCode();
    result = 31 * result + action.hashCode();
    result = 31 * result + (noValidate ? 1 : 0);
    result = 31 * result + (noReload ? 1 : 0);
    return result;
  }
}

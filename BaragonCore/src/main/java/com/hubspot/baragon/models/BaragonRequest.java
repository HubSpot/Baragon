package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonRequest {
  @NotNull
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'", flags = Pattern.Flag.MULTILINE)
  private final String loadBalancerRequestId;

  @NotNull
  @Valid
  private final BaragonService loadBalancerService;

  @NotNull
  private final List<UpstreamInfo> addUpstreams;

  @NotNull
  private final List<UpstreamInfo> removeUpstreams;

  private final Optional<String> replaceServiceId;

  private final Optional<RequestAction> action;

  @JsonCreator
  public BaragonRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                        @JsonProperty("loadBalancerService") BaragonService loadBalancerService,
                        @JsonProperty("addUpstreams") List<UpstreamInfo> addUpstreams,
                        @JsonProperty("removeUpstreams") List<UpstreamInfo> removeUpstreams,
                        @JsonProperty("replaceServiceId") Optional<String> replaceServiceId,
                        @JsonProperty("action") Optional<RequestAction> action) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = addRequestId(addUpstreams, loadBalancerRequestId);
    this.removeUpstreams = addRequestId(removeUpstreams, loadBalancerRequestId);
    this.replaceServiceId = replaceServiceId;
    this.action = action;
  }

  public BaragonRequest(String loadBalancerRequestId, BaragonService loadBalancerService, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams) {
    this(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, Optional.<String>absent(), Optional.of(RequestAction.UPDATE));
  }

  public BaragonRequest(String loadBalancerRequestId, BaragonService loadBalancerService, List<UpstreamInfo> addUpstreams, List<UpstreamInfo> removeUpstreams, Optional<String> replaceServiceId) {
    this(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, replaceServiceId, Optional.of(RequestAction.UPDATE));
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
      return new UpstreamInfo(upstream.getUpstream(), Optional.of(requestId), upstream.getRackId());
    } else {
      return upstream;
    }
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
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonRequest request = (BaragonRequest) o;

    if (!addUpstreams.equals(request.addUpstreams)) return false;
    if (!loadBalancerRequestId.equals(request.loadBalancerRequestId)) return false;
    if (!loadBalancerService.equals(request.loadBalancerService)) return false;
    if (!removeUpstreams.equals(request.removeUpstreams)) return false;
    if (!replaceServiceId.equals(request.replaceServiceId)) return false;
    if (!action.equals(request.action)) return false;

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
    return result;
  }
}

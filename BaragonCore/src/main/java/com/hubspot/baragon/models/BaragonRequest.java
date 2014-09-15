package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
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
  private final List<UpstreamInfo> addUpstreamInfo;

  @NotNull
  private final List<UpstreamInfo> removeUpstreamInfo;

  @JsonCreator
  public BaragonRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                        @JsonProperty("loadBalancerService") BaragonService loadBalancerService,
                        @JsonProperty("addUpstreams") List<String> addUpstreams,
                        @JsonProperty("removeUpstreams") List<String> removeUpstreams,
                        @JsonProperty("addUpstreamInfo") List<UpstreamInfo> addUpstreamInfo,
                        @JsonProperty("removeUpstreamInfo") List<UpstreamInfo> removeUpstreamInfo) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreamInfo = Objects.firstNonNull(addUpstreamInfo, toUpstreamInfo(addUpstreams, loadBalancerRequestId));
    this.removeUpstreamInfo = Objects.firstNonNull(removeUpstreamInfo, toUpstreamInfo(removeUpstreams, loadBalancerRequestId));
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public BaragonService getLoadBalancerService() {
    return loadBalancerService;
  }

  public List<UpstreamInfo> getAddUpstreamInfo() {
    return addUpstreamInfo;
  }

  public List<UpstreamInfo> getRemoveUpstreamInfo() {
    return removeUpstreamInfo;
  }

  private List<UpstreamInfo> toUpstreamInfo(List<String> upstreams, String loadBalancerRequestId) {
    List<UpstreamInfo> upstreamInfo = new ArrayList<>();

    if (upstreams != null) {
      for (String upstream : upstreams) {
        upstreamInfo.add(new UpstreamInfo(upstream, Optional.fromNullable(loadBalancerRequestId), Optional.<String>absent()));
      }
    }

    return upstreamInfo;
  }

  @Override
  public String toString() {
    return "BaragonRequest [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerService=" + loadBalancerService +
        ", addUpstreamInfo=" + addUpstreamInfo +
        ", removeUpstreamInfo=" + removeUpstreamInfo +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonRequest request = (BaragonRequest) o;

    if (!addUpstreamInfo.equals(request.addUpstreamInfo)) return false;
    if (!loadBalancerRequestId.equals(request.loadBalancerRequestId)) return false;
    if (!loadBalancerService.equals(request.loadBalancerService)) return false;
    if (!removeUpstreamInfo.equals(request.removeUpstreamInfo)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = loadBalancerRequestId.hashCode();
    result = 31 * result + loadBalancerService.hashCode();
    result = 31 * result + addUpstreamInfo.hashCode();
    result = 31 * result + removeUpstreamInfo.hashCode();
    return result;
  }
}

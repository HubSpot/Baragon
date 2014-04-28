package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonRequest {
  @NotEmpty
  private final String loadBalancerRequestId;

  @NotNull
  @Valid
  private final Service loadBalancerService;

  @NotNull
  private final List<String> addUpstreams;

  @NotNull
  private final List<String> removeUpstreams;

  @JsonCreator
  public BaragonRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                        @JsonProperty("loadBalancerService") Service loadBalancerService,
                        @JsonProperty("addUpstreams") List<String> addUpstreams,
                        @JsonProperty("removeUpstreams") List<String> removeUpstreams) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = addUpstreams;
    this.removeUpstreams = removeUpstreams;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public Service getLoadBalancerService() {
    return loadBalancerService;
  }

  public List<String> getAddUpstreams() {
    return addUpstreams;
  }

  public List<String> getRemoveUpstreams() {
    return removeUpstreams;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("loadBalancerRequestId", loadBalancerRequestId)
        .add("loadBalancerService", loadBalancerService)
        .add("addUpstreams", addUpstreams)
        .add("removeUpstreams", removeUpstreams)
        .toString();
  }
}

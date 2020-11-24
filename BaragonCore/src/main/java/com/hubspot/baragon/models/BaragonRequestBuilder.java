package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;

public class BaragonRequestBuilder {
  private String loadBalancerRequestId;
  private BaragonService loadBalancerService;
  private List<UpstreamInfo> addUpstreams;
  private List<UpstreamInfo> removeUpstreams;
  private List<UpstreamInfo> replaceUpstreams = Collections.<UpstreamInfo>emptyList();
  private Optional<String> replaceServiceId = Optional.<String>absent();
  private Optional<RequestAction> action = Optional.of(RequestAction.UPDATE);
  private Boolean noValidate = false;
  private Boolean noReload = false;
  private Boolean upstreamUpdateOnly = false;
  private Boolean noDuplicateUpstreams = false;
  private Boolean purgeCache = false;

  public BaragonRequestBuilder setLoadBalancerRequestId(String loadBalancerRequestId) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    return this;
  }

  public BaragonRequestBuilder setLoadBalancerService(BaragonService loadBalancerService) {
    this.loadBalancerService = loadBalancerService;
    return this;
  }

  public BaragonRequestBuilder setAddUpstreams(List<UpstreamInfo> addUpstreams) {
    this.addUpstreams = addUpstreams;
    return this;
  }

  public BaragonRequestBuilder setRemoveUpstreams(List<UpstreamInfo> removeUpstreams) {
    this.removeUpstreams = removeUpstreams;
    return this;
  }

  public BaragonRequestBuilder setReplaceUpstreams(List<UpstreamInfo> replaceUpstreams) {
    this.replaceUpstreams = replaceUpstreams;
    return this;
  }

  public BaragonRequestBuilder setAction(Optional<RequestAction> action) {
    this.action = action;
    return this;
  }

  public BaragonRequestBuilder setNoValidate(Boolean noValidate) {
    this.noValidate = noValidate;
    return this;
  }

  public BaragonRequestBuilder setNoReload(Boolean noReload) {
    this.noReload = noReload;
    return this;
  }

  public BaragonRequestBuilder setUpstreamUpdateOnly(Boolean upstreamUpdateOnly) {
    this.upstreamUpdateOnly = upstreamUpdateOnly;
    return this;
  }

  public BaragonRequestBuilder setNoDuplicateUpstreams(Boolean noDuplicateUpstreams) {
    this.noDuplicateUpstreams = noDuplicateUpstreams;
    return this;
  }

  public BaragonRequestBuilder setPurgeCache(Boolean purgeCache) {
    this.purgeCache = purgeCache;
    return this;
  }

  public BaragonRequest build() {
    return new BaragonRequest(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams, replaceUpstreams, replaceServiceId, action, noValidate, noReload, upstreamUpdateOnly, noDuplicateUpstreams, purgeCache);
  }
}

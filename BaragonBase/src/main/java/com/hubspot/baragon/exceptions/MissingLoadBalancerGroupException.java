package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.BaragonRequest;

import java.util.Collection;

public class MissingLoadBalancerGroupException extends Exception {
  private final BaragonRequest request;
  private final Collection<String> missingGroups;

  public MissingLoadBalancerGroupException(BaragonRequest request, Collection<String> missingGroups) {
    super(String.format("Request %s contains unknown load balancer groups: %s", request.getLoadBalancerRequestId(), missingGroups));
    this.request = request;
    this.missingGroups = missingGroups;
  }

  public BaragonRequest getRequest() {
    return request;
  }

  public Collection<String> getMissingGroups() {
    return missingGroups;
  }
}

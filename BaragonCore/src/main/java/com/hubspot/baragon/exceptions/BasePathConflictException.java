package com.hubspot.baragon.exceptions;

import java.util.Map;

import com.hubspot.baragon.models.BaragonRequest;

public class BasePathConflictException extends Exception {
  private final BaragonRequest request;
  private final Map<String, String> loadBalancerServiceIds;

  public BasePathConflictException(BaragonRequest request, Map<String, String> loadBalancerServiceIds) {
    super(String.format("Base path %s is already occupied on one or more load balancers: %s", request.getLoadBalancerService().getServiceBasePath(), loadBalancerServiceIds));
    this.request = request;
    this.loadBalancerServiceIds = loadBalancerServiceIds;
  }

  public BaragonRequest getRequest() {
    return request;
  }

  public Map<String, String> getLoadBalancerServiceIds() {
    return loadBalancerServiceIds;
  }
}

package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.BaragonRequest;

public class BasePathConflictException extends Exception {
  private final BaragonRequest request;
  private final String originalServiceId;

  public BasePathConflictException(BaragonRequest request, String originalServiceId) {
    super(String.format("Base path %s is already occupied by service %s", request.getLoadBalancerService().getServiceBasePath(), originalServiceId));
    this.request = request;
    this.originalServiceId = originalServiceId;
  }

  public BaragonRequest getRequest() {
    return request;
  }

  public String getOriginalServiceId() {
    return originalServiceId;
  }
}

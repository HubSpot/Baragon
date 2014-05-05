package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.BaragonRequest;

public class MissingLoadBalancerGroupException extends Exception {
  private final BaragonRequest request;

  public MissingLoadBalancerGroupException(BaragonRequest request) {
    this.request = request;
  }

  public BaragonRequest getRequest() {
    return request;
  }
}

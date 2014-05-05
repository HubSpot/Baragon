package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.BaragonRequest;

public class BasePathConflictException extends Exception {
  private final BaragonRequest request;

  public BasePathConflictException(BaragonRequest request) {
    this.request = request;
  }

  public BaragonRequest getRequest() {
    return request;
  }
}

package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.BaragonResponse;

public class RequestAlreadyEnqueuedException extends Exception {
  private final String requestId;
  private final BaragonResponse response;

  public RequestAlreadyEnqueuedException(String requestId, BaragonResponse response, String message) {
    super(message);
    this.requestId = requestId;
    this.response = response;
  }

  public String getRequestId() {
    return requestId;
  }

  public BaragonResponse getResponse() {
    return response;
  }
}

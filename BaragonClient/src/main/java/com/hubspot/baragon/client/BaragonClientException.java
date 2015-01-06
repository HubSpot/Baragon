package com.hubspot.baragon.client;

@SuppressWarnings("serial")
public class BaragonClientException extends RuntimeException {

  private int statusCode;

  public BaragonClientException() {
    super();
  }

  public BaragonClientException(String message) {
    super(message);
  }

  public BaragonClientException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public BaragonClientException(String message, Throwable t) {
    super(message, t);
  }

  public int getStatusCode() {
    return statusCode;
  }
}

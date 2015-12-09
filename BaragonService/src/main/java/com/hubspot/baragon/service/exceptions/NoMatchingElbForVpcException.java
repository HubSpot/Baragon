package com.hubspot.baragon.service.exceptions;

public class NoMatchingElbForVpcException extends Exception {
  public NoMatchingElbForVpcException(String message) {
    super(message);
  }
}

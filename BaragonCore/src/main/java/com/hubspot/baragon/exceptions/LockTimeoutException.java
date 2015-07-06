package com.hubspot.baragon.exceptions;

public class LockTimeoutException extends Exception {
  public LockTimeoutException(String heldBy) {
    super(heldBy);
  }
}
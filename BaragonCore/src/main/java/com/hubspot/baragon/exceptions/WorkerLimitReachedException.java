package com.hubspot.baragon.exceptions;

public class WorkerLimitReachedException extends Exception {
  public WorkerLimitReachedException(String message) {
    super(message);
  }
}

package com.hubspot.baragon.exceptions;

public class AgentServiceNotifyException extends Exception {
  public AgentServiceNotifyException(String message) {
    super(message);
  }

  public AgentServiceNotifyException(Exception cause) {
    super(cause);
  }
}

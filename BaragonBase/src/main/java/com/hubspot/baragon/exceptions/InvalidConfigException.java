package com.hubspot.baragon.exceptions;

public class InvalidConfigException extends RuntimeException {
  public InvalidConfigException(String output) {
    super(output);
  }
}

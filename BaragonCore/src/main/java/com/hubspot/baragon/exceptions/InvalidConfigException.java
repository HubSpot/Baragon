package com.hubspot.baragon.exceptions;

public class InvalidConfigException extends Exception {
  public InvalidConfigException(String output) {
    super(output);
  }
}

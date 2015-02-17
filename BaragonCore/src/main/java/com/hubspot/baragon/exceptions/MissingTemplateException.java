package com.hubspot.baragon.exceptions;

public class MissingTemplateException extends Exception {
  public MissingTemplateException(String output) {
    super(output);
  }
}

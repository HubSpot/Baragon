package com.hubspot.baragon.exceptions;

public class LbAdapterExecuteException extends Exception {
  private final String output;
  private final Exception executeException;

  public LbAdapterExecuteException(String output, Exception executeException) {
    super(executeException.getMessage());
    this.output = output;
    this.executeException = executeException;
  }

  public String getOutput() {
    return output;
  }

  public Exception getExecuteException() {
    return executeException;
  }
}

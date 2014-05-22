package com.hubspot.baragon.exceptions;

import org.apache.commons.exec.ExecuteException;

public class LbAdapterExecuteException extends Exception {
  private final String output;
  private final ExecuteException executeException;

  public LbAdapterExecuteException(String output, ExecuteException executeException) {
    super(executeException.getMessage());
    this.output = output;
    this.executeException = executeException;
  }

  public String getOutput() {
    return output;
  }

  public ExecuteException getExecuteException() {
    return executeException;
  }
}

package com.hubspot.baragon.exceptions;

public class LbAdapterExecuteException extends Exception {
  private final String output;
  private final Exception executeException;
  private final String command;

  public LbAdapterExecuteException(String output, Exception executeException, String command) {
    super(String.format("%s : %s", command, executeException.getMessage()));
    this.output = output;
    this.executeException = executeException;
    this.command = command;
  }

  public LbAdapterExecuteException(String output, String command) {
    super(String.format("%s : %s", command, output));
    this.output = output;
    this.command = command;
    this.executeException = null;
  }

  public String getOutput() {
    return output;
  }

  public Exception getExecuteException() {
    return executeException;
  }

  public String getCommand() {
    return command;
  }
}

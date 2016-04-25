package com.hubspot.baragon.exceptions;

public class LbAdapterRateLimitedException extends Exception {
  private final String output;
  private final String command;

  public LbAdapterRateLimitedException(String output, String command) {
    super(String.format("%s : %s", command, output));
    this.output = output;
    this.command = command;
  }

  public String getOutput() {
    return output;
  }

  public String getCommand() {
    return command;
  }
}

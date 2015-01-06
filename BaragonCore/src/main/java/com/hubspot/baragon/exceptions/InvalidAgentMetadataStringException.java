package com.hubspot.baragon.exceptions;

public class InvalidAgentMetadataStringException extends IllegalArgumentException {
  private final String agentMetadataString;

  public InvalidAgentMetadataStringException(String agentMetadataString) {
    super(String.format("'%s' is not a valid agent metadata string", agentMetadataString));
    this.agentMetadataString = agentMetadataString;
  }

  public String getAgentMetadataString() {
    return agentMetadataString;
  }
}

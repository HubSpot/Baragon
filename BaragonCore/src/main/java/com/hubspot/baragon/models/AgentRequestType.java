package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgentRequestType {
  APPLY,
  REVERT,
  CANCEL;

  @JsonCreator
  public static AgentRequestType fromString(String stringValue) {
    for (AgentRequestType value : values()) {
      if (value.name().equalsIgnoreCase(stringValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown AgentRequestType: " + stringValue);
  }
}

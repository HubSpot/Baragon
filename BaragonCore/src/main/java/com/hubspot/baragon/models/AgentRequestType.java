package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AgentRequestType {
  APPLY,
  REVERT,
  CANCEL;

  @JsonCreator
  public static AgentRequestType fromString(String value) {
    if (value == null || value.equals("")) {
      return null;
    } else {
      return valueOf(value.toUpperCase());
    }
  }
}

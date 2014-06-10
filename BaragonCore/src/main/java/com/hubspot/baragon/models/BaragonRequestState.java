package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BaragonRequestState {
  UNKNOWN, FAILED, WAITING, SUCCESS, CANCELING, CANCELED;

  @JsonCreator
  public static BaragonRequestState fromString(String value) {
    if (value == null || value.equals("")) {
      return null;
    } else {
      return valueOf(value.toUpperCase());
    }
  }
}

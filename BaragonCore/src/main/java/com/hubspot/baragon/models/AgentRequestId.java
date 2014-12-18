package com.hubspot.baragon.models;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.io.BaseEncoding;

public class AgentRequestId {
  private final AgentRequestType type;
  private final String baseUrl;

  public static AgentRequestId fromString(String value) {
    final String[] splits = value.split("\\-", 2);

    return new AgentRequestId(AgentRequestType.valueOf(splits[0]), new String(BaseEncoding.base64Url().decode(splits[1]), Charsets.UTF_8));
  }

  private AgentRequestId(AgentRequestType type, String baseUrl) {
    this.type = type;
    this.baseUrl = baseUrl;
  }

  public AgentRequestType getType() {
    return type;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", type)
        .add("baseUrl", baseUrl)
        .toString();
  }
}

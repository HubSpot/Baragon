package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  private final String baseAgentUri;
  private final Optional<String> domain;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    return new BaragonAgentMetadata(value, Optional.<String>absent());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("domain") Optional<String> domain) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
  }

  public String getBaseAgentUri() {
    return baseAgentUri;
  }

  public Optional<String> getDomain() {
    return domain;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("baseAgentUri", baseAgentUri)
        .add("domain", domain)
        .toString();
  }
}

package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  private final String baseAgentUri;
  private final Optional<String> domain;
  private final String agentId;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    Pattern pattern = Pattern.compile("http[s]?:\\/\\/([^:\\/]+:\\d{1,5})\\/");
    Matcher matcher = pattern.matcher(value);
    matcher.find();
    String agentId = matcher.group(1);
    return new BaragonAgentMetadata(value, agentId, Optional.<String>absent());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("agentId") String agentId,
                              @JsonProperty("domain") Optional<String> domain) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.agentId = agentId;
  }

  public String getBaseAgentUri() {
    return baseAgentUri;
  }

  public Optional<String> getDomain() {
    return domain;
  }

  public String getAgentId() {
    return agentId;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("baseAgentUri", baseAgentUri)
        .add("agentId", agentId)
        .add("domain", domain)
        .toString();
  }
}

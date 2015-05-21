package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  public static final Pattern AGENT_METADATA_STRING_REGEX = Pattern.compile("^http[s]?:\\/\\/([^:\\/]+:\\d{1,5})\\/.*$");

  private final String baseAgentUri;
  private final Optional<String> domain;
  private final String agentId;
  private final Optional<String> instanceId;
  private final Optional<String> availabilityZone;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    final Matcher matcher = AGENT_METADATA_STRING_REGEX.matcher(value);

    if (!matcher.matches()) {
      throw new InvalidAgentMetadataStringException(value);
    }

    return new BaragonAgentMetadata(value, matcher.group(1), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("agentId") String agentId,
                              @JsonProperty("domain") Optional<String> domain,
                              @JsonProperty("instanceId") Optional<String> instanceId,
                              @JsonProperty("availabilityZone") Optional<String> availabilityZone) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.agentId = agentId;
    this.instanceId = instanceId;
    this.availabilityZone = availabilityZone;
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

  public Optional<String> getInstanceId() {
    return instanceId;
  }

  public Optional<String> getAvailabilityZone() {
    return availabilityZone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonAgentMetadata metadata = (BaragonAgentMetadata) o;

    if (agentId != null ? !agentId.equals(metadata.agentId) : metadata.agentId != null) {
      return false;
    }
    if (!baseAgentUri.equals(metadata.baseAgentUri)) {
      return false;
    }
    if (!domain.equals(metadata.domain)) {
      return false;
    }
    if (!instanceId.equals(metadata.instanceId)) {
      return false;
    }
    if (!availabilityZone.equals(metadata.availabilityZone)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = baseAgentUri.hashCode();
    result = 31 * result + domain.hashCode();
    result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
    result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
    result = 31 * result + (availabilityZone != null ? availabilityZone.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("baseAgentUri", baseAgentUri)
            .add("domain", domain)
            .add("agentId", agentId)
            .add("instanceId", instanceId)
            .add("availabilityZone", availabilityZone)
            .toString();
  }
}

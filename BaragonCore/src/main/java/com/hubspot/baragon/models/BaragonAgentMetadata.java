package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  public static final Pattern AGENT_METADATA_STRING_REGEX = Pattern.compile("^http[s]?:\\/\\/([^:\\/]+:\\d{1,5})\\/.*$");

  private final String baseAgentUri;
  private final Optional<String> domain;
  private final String agentId;
  private final BaragonAgentEc2Metadata ec2;
  private final Map<String, String> extraAgentData;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    final Matcher matcher = AGENT_METADATA_STRING_REGEX.matcher(value);

    if (!matcher.matches()) {
      throw new InvalidAgentMetadataStringException(value);
    }

    return new BaragonAgentMetadata(value, matcher.group(1), Optional.<String>absent(), new BaragonAgentEc2Metadata(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent()), Collections.<String, String>emptyMap());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("agentId") String agentId,
                              @JsonProperty("domain") Optional<String> domain,
                              @JsonProperty("ec2") BaragonAgentEc2Metadata ec2,
                              @JsonProperty("extraAgentData") Map<String, String> extraAgentData) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.agentId = agentId;
    this.ec2 = ec2;
    this.extraAgentData = Objects.firstNonNull(extraAgentData, Collections.<String, String>emptyMap());
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

  public BaragonAgentEc2Metadata getEc2() {
    return ec2;
  }

  public Map<String, String> getExtraAgentData() {
    return extraAgentData;
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
    if (!ec2.equals(metadata.ec2)) {
      return false;
    }
    if (!extraAgentData.equals(metadata.extraAgentData)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = baseAgentUri.hashCode();
    result = 31 * result + domain.hashCode();
    result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
    result = 31 * result + (ec2 != null ? ec2.hashCode() : 0);
    result = 31 * result + extraAgentData.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("baseAgentUri", baseAgentUri)
            .add("domain", domain)
            .add("agentId", agentId)
            .add("ec2", ec2)
            .add("extraAgentData", extraAgentData)
            .toString();
  }
}

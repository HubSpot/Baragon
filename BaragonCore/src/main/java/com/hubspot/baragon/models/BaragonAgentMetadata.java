package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  public static final Pattern AGENT_METADATA_STRING_REGEX = Pattern.compile("^http[s]?:\\/\\/([^:\\/]+:\\d{1,5})\\/.*$");

  private final String baseAgentUri;
  @Deprecated
  private final Optional<String> domain;
  private final String agentId;
  private final BaragonAgentEc2Metadata ec2;
  private final Map<String, String> extraAgentData;
  private final boolean batchEnabled;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    final Matcher matcher = AGENT_METADATA_STRING_REGEX.matcher(value);

    if (!matcher.matches()) {
      throw new InvalidAgentMetadataStringException(value);
    }

    return new BaragonAgentMetadata(value, matcher.group(1), Optional.<String>absent(), new BaragonAgentEc2Metadata(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent()), Collections.<String, String>emptyMap(), false);
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("agentId") String agentId,
                              @JsonProperty("domain") Optional<String> domain,
                              @JsonProperty("ec2") BaragonAgentEc2Metadata ec2,
                              @JsonProperty("extraAgentData") Map<String, String> extraAgentData,
                              @JsonProperty("batchEnabled") boolean batchEnabled) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.agentId = agentId;
    this.ec2 = ec2;
    this.extraAgentData = MoreObjects.firstNonNull(extraAgentData, Collections.<String, String>emptyMap());
    this.batchEnabled = MoreObjects.firstNonNull(batchEnabled, false);
  }

  public String getBaseAgentUri() {
    return baseAgentUri;
  }

  @Deprecated
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

  public boolean isBatchEnabled() {
    return batchEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaragonAgentMetadata that = (BaragonAgentMetadata) o;
    return batchEnabled == that.batchEnabled &&
      Objects.equal(baseAgentUri, that.baseAgentUri) &&
      Objects.equal(domain, that.domain) &&
      Objects.equal(agentId, that.agentId) &&
      Objects.equal(ec2, that.ec2) &&
      Objects.equal(extraAgentData, that.extraAgentData) &&
      Objects.equal(batchEnabled, batchEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(baseAgentUri, domain, agentId, ec2, extraAgentData, batchEnabled);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("baseAgentUri", baseAgentUri)
      .add("domain", domain)
      .add("agentId", agentId)
      .add("ec2", ec2)
      .add("extraAgentData", extraAgentData)
      .add("batchEnabled", batchEnabled)
      .toString();
  }
}

package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
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
  private final Optional<BaragonAgentGcloudMetadata> gcloud;
  private final Map<String, String> extraAgentData;
  private final boolean batchEnabled;
  private final Optional<Integer> trafficPort;
  private final Optional<Integer> sslTrafficPort;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    final Matcher matcher = AGENT_METADATA_STRING_REGEX.matcher(value);

    if (!matcher.matches()) {
      throw new InvalidAgentMetadataStringException(value);
    }

    return new BaragonAgentMetadata(value, matcher.group(1), Optional.absent(), new BaragonAgentEc2Metadata(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections.emptyMap(), false, Optional.absent(), Optional.absent());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("agentId") String agentId,
                              @JsonProperty("domain") Optional<String> domain,
                              @JsonProperty("ec2") BaragonAgentEc2Metadata ec2,
                              @JsonProperty("gcloud") Optional<BaragonAgentGcloudMetadata> gcloud,
                              @JsonProperty("extraAgentData") Map<String, String> extraAgentData,
                              @JsonProperty("batchEnabled") boolean batchEnabled,
                              @JsonProperty("trafficPort") Optional<Integer> trafficPort,
                              @JsonProperty("sslTrafficPort") Optional<Integer> sslTrafficPort) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.agentId = agentId;
    this.ec2 = ec2;
    this.gcloud = gcloud;
    this.extraAgentData = MoreObjects.firstNonNull(extraAgentData, Collections.<String, String>emptyMap());
    this.batchEnabled = MoreObjects.firstNonNull(batchEnabled, false);
    this.trafficPort = trafficPort;
    this.sslTrafficPort = sslTrafficPort;
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

  public Optional<BaragonAgentGcloudMetadata> getGcloud() {
    return gcloud;
  }

  public Map<String, String> getExtraAgentData() {
    return extraAgentData;
  }

  public boolean isBatchEnabled() {
    return batchEnabled;
  }

  public Optional<Integer> getTrafficPort() {
    return trafficPort;
  }

  public Optional<Integer> getSslTrafficPort() {
    return sslTrafficPort;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BaragonAgentMetadata) {
      final BaragonAgentMetadata that = (BaragonAgentMetadata) obj;
      return Objects.equals(this.batchEnabled, that.batchEnabled) &&
          Objects.equals(this.baseAgentUri, that.baseAgentUri) &&
          Objects.equals(this.domain, that.domain) &&
          Objects.equals(this.agentId, that.agentId) &&
          Objects.equals(this.ec2, that.ec2) &&
          Objects.equals(this.gcloud, that.gcloud) &&
          Objects.equals(this.extraAgentData, that.extraAgentData) &&
          Objects.equals(this.trafficPort, that.trafficPort) &&
          Objects.equals(this.sslTrafficPort, that.sslTrafficPort);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseAgentUri, domain, agentId, ec2, gcloud, extraAgentData, batchEnabled, trafficPort, sslTrafficPort);
  }

  @Override
  public String toString() {
    return "BaragonAgentMetadata{" +
        "baseAgentUri='" + baseAgentUri + '\'' +
        ", domain=" + domain +
        ", agentId='" + agentId + '\'' +
        ", ec2=" + ec2 +
        ", gcloud=" + gcloud +
        ", extraAgentData=" + extraAgentData +
        ", batchEnabled=" + batchEnabled +
        ", trafficPort=" + trafficPort +
        ", sslTrafficPort=" + sslTrafficPort +
        '}';
  }
}

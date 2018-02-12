package com.hubspot.baragon.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonKnownAgentMetadata extends BaragonAgentMetadata {
  private long lastSeenAt;

  public static BaragonKnownAgentMetadata fromAgentMetadata(BaragonAgentMetadata agentMetadata, long lastSeenAt) {
    return new BaragonKnownAgentMetadata(agentMetadata.getBaseAgentUri(), agentMetadata.getAgentId(), agentMetadata.getDomain(), agentMetadata.getEc2(), agentMetadata.getGcloud(), agentMetadata.getExtraAgentData(), agentMetadata.isBatchEnabled(), lastSeenAt);
  }

  @JsonCreator
  public BaragonKnownAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                                   @JsonProperty("agentId") String agentId,
                                   @JsonProperty("domain") Optional<String> domain,
                                   @JsonProperty("ec2") BaragonAgentEc2Metadata ec2,
                                   @JsonProperty("gcloud") Optional<BaragonAgentGcloudMetadata> gcloud,
                                   @JsonProperty("extraAgentData")Map<String, String> extraAgentData,
                                   @JsonProperty("batchEnabled") boolean batchEnabled,
                                   @JsonProperty("lastSeenAt") long lastSeenAt) {
    super(baseAgentUri, agentId, domain, ec2, gcloud, extraAgentData, batchEnabled);
    this.lastSeenAt = lastSeenAt;
  }

  public long getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(long lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    BaragonKnownAgentMetadata that = (BaragonKnownAgentMetadata) o;

    if (lastSeenAt != that.lastSeenAt) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (lastSeenAt ^ (lastSeenAt >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("baseAgentUri", getBaseAgentUri())
            .add("domain", getDomain())
            .add("agentId", getAgentId())
            .add("lastSeenAt", lastSeenAt)
            .toString();
  }
}

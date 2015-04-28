package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonKnownAgentMetadata extends BaragonAgentMetadata {
  private final long lastSeenAt;

  public static BaragonKnownAgentMetadata fromAgentMetadata(BaragonAgentMetadata agentMetadata, long firstSeenAt) {
    return new BaragonKnownAgentMetadata(agentMetadata.getBaseAgentUri(), agentMetadata.getAgentId(), agentMetadata.getDomain(), agentMetadata.getInstanceId(), firstSeenAt);
  }

  @JsonCreator
  public BaragonKnownAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                                   @JsonProperty("agentId") String agentId,
                                   @JsonProperty("domain") Optional<String> domain,
                                   @JsonProperty("instanceId") Optional<String> instanceId,
                                   @JsonProperty("lastSeenAt") long lastSeenAt) {
    super(baseAgentUri, agentId, domain, instanceId);
    this.lastSeenAt = lastSeenAt;
  }

  public long getLastSeenAt() {
    return lastSeenAt;
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
    return Objects.toStringHelper(this)
            .add("baseAgentUri", getBaseAgentUri())
            .add("domain", getDomain())
            .add("agentId", getAgentId())
            .add("lastSeenAt", lastSeenAt)
            .toString();
  }
}

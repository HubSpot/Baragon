package com.hubspot.baragon.models;

import java.util.List;

import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentEc2Metadata {
  private final Optional<String> instanceId;
  private final Optional<String> availabilityZone;
  private final Optional<String> subnetId;

  @JsonCreator
  public BaragonAgentEc2Metadata(@JsonProperty("instanceId") Optional<String> instanceId,
                                 @JsonProperty("availabilityZone") Optional<String> availabilityZone,
                                 @JsonProperty("subnetId") Optional<String> subnetId) {
    this.instanceId = instanceId;
    this.availabilityZone = availabilityZone;
    this.subnetId = subnetId;
  }

  public static BaragonAgentEc2Metadata fromEnvironment() {
    return new BaragonAgentEc2Metadata(
      findInstanceId(),
      findAvailabilityZone(),
      findSubnet());
  }

  public static Optional<String> findInstanceId() {
    try {
      return Optional.fromNullable(EC2MetadataUtils.getInstanceId());
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  public static Optional<String> findAvailabilityZone() {
    try {
      return Optional.fromNullable(EC2MetadataUtils.getAvailabilityZone());
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  private static Optional<String> findSubnet() {
    try {
      List<EC2MetadataUtils.NetworkInterface> networkInterfaces = EC2MetadataUtils.getNetworkInterfaces();
      if (EC2MetadataUtils.getNetworkInterfaces().isEmpty()) {
        return Optional.absent();
      } else {
        return Optional.fromNullable(networkInterfaces.get(0).getSubnetId());
      }
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  public Optional<String> getInstanceId() {
    return instanceId;
  }

  public Optional<String> getAvailabilityZone() {
    return availabilityZone;
  }

  public Optional<String> getSubnetId() {
    return subnetId;
  }
}

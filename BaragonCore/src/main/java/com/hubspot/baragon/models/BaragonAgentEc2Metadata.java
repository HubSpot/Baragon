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
  private final Optional<String> vpcId;

  @JsonCreator
  public BaragonAgentEc2Metadata(@JsonProperty("instanceId") Optional<String> instanceId,
                                 @JsonProperty("availabilityZone") Optional<String> availabilityZone,
                                 @JsonProperty("subnetId") Optional<String> subnetId,
                                 @JsonProperty("vpcId") Optional<String> vpcId) {
    this.instanceId = instanceId;
    this.availabilityZone = availabilityZone;
    this.subnetId = subnetId;
    this.vpcId = vpcId;
  }

  public static BaragonAgentEc2Metadata fromEnvironment() {
    return new BaragonAgentEc2Metadata(
      findInstanceId(),
      findAvailabilityZone(),
      findSubnet(),
      findVpc());
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

  private static Optional<String> findVpc() {
    try {
      List<EC2MetadataUtils.NetworkInterface> networkInterfaces = EC2MetadataUtils.getNetworkInterfaces();
      if (EC2MetadataUtils.getNetworkInterfaces().isEmpty()) {
        return Optional.absent();
      } else {
        return Optional.fromNullable(networkInterfaces.get(0).getVpcId());
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

  public Optional<String> getVpcId() {
    return vpcId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonAgentEc2Metadata that = (BaragonAgentEc2Metadata) o;

    if (instanceId != null ? !instanceId.equals(that.instanceId) : that.instanceId != null) {
      return false;
    }
    if (availabilityZone != null ? !availabilityZone.equals(that.availabilityZone) : that.availabilityZone != null) {
      return false;
    }
    if (subnetId != null ? !subnetId.equals(that.subnetId) : that.subnetId != null) {
      return false;
    }
    return vpcId != null ? vpcId.equals(that.vpcId) : that.vpcId == null;

  }

  @Override
  public int hashCode() {
    int result = instanceId != null ? instanceId.hashCode() : 0;
    result = 31 * result + (availabilityZone != null ? availabilityZone.hashCode() : 0);
    result = 31 * result + (subnetId != null ? subnetId.hashCode() : 0);
    result = 31 * result + (vpcId != null ? vpcId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "BaragonAgentEc2Metadata{" +
      "instanceId=" + instanceId +
      ", availabilityZone=" + availabilityZone +
      ", subnetId=" + subnetId +
      ", vpcId=" + vpcId +
      '}';
  }
}

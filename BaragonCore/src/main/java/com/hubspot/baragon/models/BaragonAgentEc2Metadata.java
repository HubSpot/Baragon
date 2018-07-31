package com.hubspot.baragon.models;

import java.util.List;
import java.util.Objects;

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
  private final Optional<String> privateIp;

  @JsonCreator
  public BaragonAgentEc2Metadata(@JsonProperty("instanceId") Optional<String> instanceId,
                                 @JsonProperty("availabilityZone") Optional<String> availabilityZone,
                                 @JsonProperty("subnetId") Optional<String> subnetId,
                                 @JsonProperty("vpcId") Optional<String> vpcId,
                                 @JsonProperty("privateIp") Optional<String> privateIp) {
    this.instanceId = instanceId;
    this.availabilityZone = availabilityZone;
    this.subnetId = subnetId;
    this.vpcId = vpcId;
    this.privateIp = privateIp;
  }

  public static BaragonAgentEc2Metadata fromEnvironment() {
    return new BaragonAgentEc2Metadata(
      findInstanceId(),
      findAvailabilityZone(),
      findSubnet(),
      findVpc(),
      findPrivateIp());
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

  private static Optional<String> findPrivateIp() {
    try {
      return Optional.fromNullable(EC2MetadataUtils.getPrivateIpAddress());
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

  public Optional<String> getPrivateIp() {
    return privateIp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BaragonAgentEc2Metadata) {
      final BaragonAgentEc2Metadata that = (BaragonAgentEc2Metadata) obj;
      return Objects.equals(this.instanceId, that.instanceId) &&
          Objects.equals(this.availabilityZone, that.availabilityZone) &&
          Objects.equals(this.subnetId, that.subnetId) &&
          Objects.equals(this.vpcId, that.vpcId) &&
          Objects.equals(this.privateIp, that.privateIp);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, availabilityZone, subnetId, vpcId, privateIp);
  }

  @Override
  public String toString() {
    return "BaragonAgentEc2Metadata{" +
        "instanceId=" + instanceId +
        ", availabilityZone=" + availabilityZone +
        ", subnetId=" + subnetId +
        ", vpcId=" + vpcId +
        ", privateIp=" + privateIp +
        '}';
  }
}

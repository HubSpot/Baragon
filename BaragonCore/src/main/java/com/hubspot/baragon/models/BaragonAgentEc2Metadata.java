package com.hubspot.baragon.models;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentEc2Metadata {
  private final Optional<String> instanceId;
  private final Optional<String> availabilityZone;
  private final Optional<String> subnetId;

  private static final String INSTANCE_ID_URL = "http://169.254.169.254/latest/meta-data/instance-id";
  private static final String AVAILABILITY_ZONE_URL = "http://169.254.169.254/latest/meta-data/placement/availability-zone";
  private static final String MACS_URL = "http://169.254.169.254/latest/meta-data/network/interfaces/macs/";
  private static final String SUBNET_URL_FORMAT = "http://169.254.169.254/latest/meta-data/network/interfaces/macs/%ssubnet-id";

  @JsonCreator
  public BaragonAgentEc2Metadata(@JsonProperty("instanceId") Optional<String> instanceId,
                                 @JsonProperty("availabilityZone") Optional<String> availabilityZone,
                                 @JsonProperty("subnetId") Optional<String> subnetId) {
    this.instanceId = instanceId;
    this.availabilityZone = availabilityZone;
    this.subnetId = subnetId;
  }

  public static BaragonAgentEc2Metadata fromEnvironment() {
    return new BaragonAgentEc2Metadata(getEc2Metadata(INSTANCE_ID_URL), getEc2Metadata(AVAILABILITY_ZONE_URL), getSubnetFromEnv());
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

  private static Optional<String> getSubnetFromEnv() {
    Optional<String> mac = getEc2Metadata(MACS_URL);
    return getEc2Metadata(String.format(SUBNET_URL_FORMAT, mac));
  }

  private static Optional<String> getEc2Metadata(String url) {
    try {
      String instanceId = null;
      String inputLine;
      URL ec2MetaData = new URL(url);
      URLConnection ec2Conn = ec2MetaData.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(ec2Conn.getInputStream(), "UTF-8"));
      while ((inputLine = in.readLine()) != null) {
        instanceId = inputLine;
      }
      in.close();
      return Optional.fromNullable(instanceId);
    } catch (Exception e) {
      return Optional.absent();
    }
  }
}

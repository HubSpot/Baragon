package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {
  @NotEmpty
  private final String serviceId;

  @NotNull
  private final Collection<String> owners;

  @NotEmpty
  private final String loadBalancerBaseUri;

  @NotEmpty
  private final List<String> loadBalancerGroups;

  @NotNull
  private final Map<String, Object> options;
  
  public Service(@JsonProperty("serviceId") String serviceId,
                 @JsonProperty("owners") Collection<String> owners,
                 @JsonProperty("loadBalancerBaseUri") String loadBalancerBaseUri,
                 @JsonProperty("loadBalancerGroups") List<String> loadBalancerGroups,
                 @JsonProperty("options") Map<String, Object> options) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.loadBalancerBaseUri = loadBalancerBaseUri;
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;

    // TODO: bring back rewriteAppRootToo (sorry Ian)
  }

  public String getServiceId() {
    return serviceId;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public String getLoadBalancerBaseUri() {
    return loadBalancerBaseUri;
  }

  public List<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Service.class)
        .add("serviceId", serviceId)
        .add("owners", owners)
        .add("loadBalancerBaseUri", loadBalancerBaseUri)
        .add("loadBalancerGroups", loadBalancerGroups)
        .add("options", options)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceId, owners, loadBalancerBaseUri, loadBalancerGroups, options);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof Service) {
      return Objects.equal(serviceId, ((Service)that).getServiceId())
          && Objects.equal(owners, ((Service)that).getOwners())
          && Objects.equal(loadBalancerBaseUri, ((Service)that).getLoadBalancerBaseUri())
          && Objects.equal(loadBalancerGroups, ((Service)that).getLoadBalancerGroups())
          && Objects.equal(options, ((Service)that).getOptions());
    }

    return false;
  }
}

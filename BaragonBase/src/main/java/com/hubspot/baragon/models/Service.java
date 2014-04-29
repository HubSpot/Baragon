package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import io.dropwizard.validation.ValidationMethod;
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
  private final String serviceBasePath;

  @NotEmpty
  private final List<String> loadBalancerGroups;

  @NotNull
  private final Map<String, Object> options;
  
  public Service(@JsonProperty("serviceId") String serviceId,
                 @JsonProperty("owners") Collection<String> owners,
                 @JsonProperty("serviceBasePath") String serviceBasePath,
                 @JsonProperty("loadBalancerBaseUri") String loadBalancerBaseUri,  // <-- this is going away soon
                 @JsonProperty("loadBalancerGroups") List<String> loadBalancerGroups,
                 @JsonProperty("options") Map<String, Object> options) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = !Strings.isNullOrEmpty(serviceBasePath) ? serviceBasePath : loadBalancerBaseUri;
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;

    // TODO: bring back rewriteAppRootTo (sorry Ian)
  }

  public String getServiceId() {
    return serviceId;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public String getServiceBasePath() {
    return serviceBasePath;
  }

  @JsonIgnore
  @Deprecated
  public String getLoadBalancerBaseUri() {
    return serviceBasePath;
  }

  public List<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @ValidationMethod(message = "serviceBasePath must be a valid absolute path")
  @JsonIgnore
  public boolean validServiceBasePath() {
    return !Strings.isNullOrEmpty(serviceBasePath) && serviceBasePath.startsWith("/");  // TODO: make this more rigorous
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Service.class)
        .add("serviceId", serviceId)
        .add("owners", owners)
        .add("serviceBasePath", serviceBasePath)
        .add("loadBalancerGroups", loadBalancerGroups)
        .add("options", options)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceId, owners, serviceBasePath, loadBalancerGroups, options);
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
          && Objects.equal(serviceBasePath, ((Service)that).getServiceBasePath())
          && Objects.equal(loadBalancerGroups, ((Service)that).getLoadBalancerGroups())
          && Objects.equal(options, ((Service)that).getOptions());
    }

    return false;
  }
}

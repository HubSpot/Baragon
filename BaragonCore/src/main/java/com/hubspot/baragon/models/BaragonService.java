package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonService {
  @NotNull
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'", flags = Pattern.Flag.MULTILINE)
  private final String serviceId;

  @NotNull
  private final Collection<String> owners;

  @NotNull
  @Pattern(regexp = "/(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9A-Fa-f]{2})*", message = "must be an absolute URL path")
  private final String serviceBasePath;

  @NotNull
  @Size(min=1)
  private final List<String> loadBalancerGroups;

  private final Map<String, Object> options;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("loadBalancerGroups") List<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
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

  public List<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return "Service [" +
        "serviceId='" + serviceId + '\'' +
        ", owners=" + owners +
        ", serviceBasePath='" + serviceBasePath + '\'' +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", options=" + options +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonService service = (BaragonService) o;

    if (loadBalancerGroups != null ? !loadBalancerGroups.equals(service.loadBalancerGroups) : service.loadBalancerGroups != null)
      return false;
    if (options != null ? !options.equals(service.options) : service.options != null) return false;
    if (owners != null ? !owners.equals(service.owners) : service.owners != null) return false;
    if (serviceBasePath != null ? !serviceBasePath.equals(service.serviceBasePath) : service.serviceBasePath != null)
      return false;
    if (serviceId != null ? !serviceId.equals(service.serviceId) : service.serviceId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = serviceId != null ? serviceId.hashCode() : 0;
    result = 31 * result + (owners != null ? owners.hashCode() : 0);
    result = 31 * result + (serviceBasePath != null ? serviceBasePath.hashCode() : 0);
    result = 31 * result + (loadBalancerGroups != null ? loadBalancerGroups.hashCode() : 0);
    result = 31 * result + (options != null ? options.hashCode() : 0);
    return result;
  }
}

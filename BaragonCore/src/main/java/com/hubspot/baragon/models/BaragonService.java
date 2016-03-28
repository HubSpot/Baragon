package com.hubspot.baragon.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonService {
  @NotNull
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'", flags = Pattern.Flag.MULTILINE)
  private final String serviceId;

  @NotNull
  private final Collection<String> owners;

  @NotNull
  private final String serviceBasePath;

  private final List<String> additionalPaths;

  @NotNull
  @Size(min=1)
  private final Set<String> loadBalancerGroups;

  private final Map<String, Object> options;

  private final Optional<String> templateName;

  private final Optional<String> domain;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("additionalPaths") List<String> additionalPaths,
                        @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options,
                        @JsonProperty("templateName") Optional<String> templateName,
                        @JsonProperty("domain") Optional<String> domain) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.additionalPaths = Objects.firstNonNull(additionalPaths, Collections.<String> emptyList());
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
    this.templateName = templateName;
    this.domain = domain;
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, Set<String> loadBalancerGroups, Map<String, Object> options) {
    this(serviceId, owners, serviceBasePath, Collections.<String>emptyList(), loadBalancerGroups, options, Optional.<String>absent(), Optional.<String>absent());
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

  public List<String> getAdditionalPaths() {
    return additionalPaths;
  }

  public Set<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public Optional<String> getTemplateName() {
    return templateName;
  }

  public Optional<String> getDomain() {
    return domain;
  }

  @JsonIgnore
  public List<String> getAllPaths() {
    List<String> allPaths = new ArrayList<>();
    allPaths.addAll(additionalPaths);
    allPaths.add(serviceBasePath);
    return allPaths;
  }

  @Override
  public String toString() {
    return "BaragonService [" +
        "serviceId='" + serviceId + '\'' +
        ", owners=" + owners +
        ", serviceBasePath='" + serviceBasePath + '\'' +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", options=" + options +
        ", templateName=" + templateName +
        ", domain=" + domain +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonService service = (BaragonService) o;

    if (loadBalancerGroups != null ? !loadBalancerGroups.equals(service.loadBalancerGroups) : service.loadBalancerGroups != null) {
      return false;
    }
    if (options != null ? !options.equals(service.options) : service.options != null) {
      return false;
    }
    if (owners != null ? !owners.equals(service.owners) : service.owners != null) {
      return false;
    }
    if (serviceBasePath != null ? !serviceBasePath.equals(service.serviceBasePath) : service.serviceBasePath != null) {
      return false;
    }
    if (additionalPaths != null ? !additionalPaths.equals(service.additionalPaths) : service.additionalPaths != null) {
      return false;
    }
    if (serviceId != null ? !serviceId.equals(service.serviceId) : service.serviceId != null) {
      return false;
    }
    if (templateName != null ? !templateName.equals(service.templateName) : service.templateName != null) {
      return false;
    }
    if (domain != null ? !domain.equals(service.domain) : service.domain != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = serviceId != null ? serviceId.hashCode() : 0;
    result = 31 * result + (owners != null ? owners.hashCode() : 0);
    result = 31 * result + (serviceBasePath != null ? serviceBasePath.hashCode() : 0);
    result = 31 * result + (additionalPaths != null ? additionalPaths.hashCode() : 0);
    result = 31 * result + (loadBalancerGroups != null ? loadBalancerGroups.hashCode() : 0);
    result = 31 * result + (options != null ? options.hashCode() : 0);
    result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
    result = 31 * result + (domain != null ? domain.hashCode() : 0);
    return result;
  }
}

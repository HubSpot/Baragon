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
import com.google.common.base.MoreObjects;
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

  private final Set<String> domains;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("additionalPaths") List<String> additionalPaths,
                        @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options,
                        @JsonProperty("templateName") Optional<String> templateName,
                        @JsonProperty("domains") Set<String> domains) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.additionalPaths = MoreObjects.firstNonNull(additionalPaths, Collections.<String> emptyList());
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
    this.templateName = templateName;
    this.domains = MoreObjects.firstNonNull(domains, Collections.<String>emptySet());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, Collections.<String>emptySet());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, Set<String> loadBalancerGroups, Map<String, Object> options) {
    this(serviceId, owners, serviceBasePath, Collections.<String>emptyList(), loadBalancerGroups, options, Optional.<String>absent(), Collections.<String>emptySet());
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

  public Set<String> getDomains() {
    return domains;
  }

  @JsonIgnore
  public List<String> getAllPaths() {
    List<String> allPaths = new ArrayList<>();
    for (String path : additionalPaths) {
      if (!domains.isEmpty()) {
        for (String domain : domains) {
          allPaths.add(String.format("%s%s", domain, path));
        }
      } else {
        allPaths.add(path);
      }
    }
    if (!domains.isEmpty()) {
      for (String domain : domains) {
        allPaths.add(String.format("%s%s", domain, serviceBasePath));
      }
    } else {
      allPaths.add(serviceBasePath);
    }
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
        ", domains=" + domains +
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
    if (domains != null ? !domains.equals(service.domains) : service.domains != null) {
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
    result = 31 * result + (domains != null ? domains.hashCode() : 0);
    return result;
  }
}

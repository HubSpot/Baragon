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

  private final Set<String> domains;

  private final Optional<Double> priority;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("additionalPaths") List<String> additionalPaths,
                        @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options,
                        @JsonProperty("templateName") Optional<String> templateName,
                        @JsonProperty("domains") Set<String> domains,
                        @JsonProperty("priority") Optional<Double> priority) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.additionalPaths = Objects.firstNonNull(additionalPaths, Collections.<String> emptyList());
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
    this.templateName = templateName;
    this.domains = Objects.firstNonNull(domains, Collections.<String>emptySet());
    this.priority = priority;
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, Collections.<String>emptySet(), Optional.<Double>absent());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, Set<String> loadBalancerGroups, Map<String, Object> options) {
    this(serviceId, owners, serviceBasePath, Collections.<String>emptyList(), loadBalancerGroups, options, Optional.<String>absent(), Collections.<String>emptySet(), Optional.<Double>absent());
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

  public Optional<Double> getPriority() {
    return priority;
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaragonService that = (BaragonService) o;
    return Objects.equal(serviceId, that.serviceId) &&
      Objects.equal(owners, that.owners) &&
      Objects.equal(serviceBasePath, that.serviceBasePath) &&
      Objects.equal(additionalPaths, that.additionalPaths) &&
      Objects.equal(loadBalancerGroups, that.loadBalancerGroups) &&
      Objects.equal(options, that.options) &&
      Objects.equal(templateName, that.templateName) &&
      Objects.equal(domains, that.domains) &&
      Objects.equal(priority, that.priority);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, priority);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("serviceId", serviceId)
      .add("owners", owners)
      .add("serviceBasePath", serviceBasePath)
      .add("additionalPaths", additionalPaths)
      .add("loadBalancerGroups", loadBalancerGroups)
      .add("options", options)
      .add("templateName", templateName)
      .add("domains", domains)
      .add("priority", priority)
      .toString();
  }

}

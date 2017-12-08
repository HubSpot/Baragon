package com.hubspot.baragon.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

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

  @Deprecated
  private final Optional<String> edgeCacheDNS;

  private final Set<String> edgeCacheDomains;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("additionalPaths") List<String> additionalPaths,
                        @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options,
                        @JsonProperty("templateName") Optional<String> templateName,
                        @JsonProperty("domains") Set<String> domains,
                        @JsonProperty("edgeCacheDNS") Optional<String> edgeCacheDNS,
                        @JsonProperty("edgeCacheDomains") Set<String> edgeCacheDomains) {
    this.serviceId = serviceId;
    this.owners = owners;
    this.serviceBasePath = serviceBasePath;
    this.additionalPaths = MoreObjects.firstNonNull(additionalPaths, Collections.<String> emptyList());
    this.loadBalancerGroups = loadBalancerGroups;
    this.options = options;
    this.templateName = templateName;
    this.domains = MoreObjects.firstNonNull(domains, Collections.<String>emptySet());
    this.edgeCacheDNS = edgeCacheDNS;
    Set<String> edgeDomains = edgeCacheDomains != null ? Sets.newHashSet(edgeCacheDomains) : new HashSet<>();
    if (edgeCacheDNS.isPresent()) {
      edgeDomains.add(edgeCacheDNS.get());
    }
    this.edgeCacheDomains = edgeDomains;
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options,
                        Optional<String> templateName, Set<String> domains, Optional<String> edgeCacheDNS) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, Collections.emptySet());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options,
                        Optional<String> templateName, Set<String> domains, Set<String> edgeCacheDomains) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, Optional.absent(), edgeCacheDomains);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName, Set<String> domains) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, Optional.absent(), Collections.emptySet());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, Collections.<String>emptySet(), Optional.absent(), Collections.emptySet());
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, Set<String> loadBalancerGroups, Map<String, Object> options) {
    this(serviceId, owners, serviceBasePath, Collections.<String>emptyList(), loadBalancerGroups, options, Optional.<String>absent(), Collections.<String>emptySet(), Optional.absent(), Collections.emptySet());
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

  /**
   * Data from this field is primarily used to populate data in rendered nginx config templates.
   */
  public Map<String, Object> getOptions() {
    return options;
  }

  public Optional<String> getTemplateName() {
    return templateName;
  }

  public Set<String> getDomains() {
    return domains;
  }

  public Optional<String> getEdgeCacheDNS() {
    return edgeCacheDNS;
  }

  public Set<String> getEdgeCacheDomains() {
    return edgeCacheDomains;
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
    return "BaragonService{" +
        "serviceId='" + serviceId + '\'' +
        ", owners=" + owners +
        ", serviceBasePath='" + serviceBasePath + '\'' +
        ", additionalPaths=" + additionalPaths +
        ", loadBalancerGroups=" + loadBalancerGroups +
        ", options=" + options +
        ", templateName=" + templateName +
        ", domains=" + domains +
        ", edgeCacheDNS=" + edgeCacheDNS +
        ", edgeCacheDomains=" + edgeCacheDomains +
        '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BaragonService) {
      final BaragonService that = (BaragonService) obj;
      return Objects.equals(this.serviceId, that.serviceId) &&
          Objects.equals(this.owners, that.owners) &&
          Objects.equals(this.serviceBasePath, that.serviceBasePath) &&
          Objects.equals(this.additionalPaths, that.additionalPaths) &&
          Objects.equals(this.loadBalancerGroups, that.loadBalancerGroups) &&
          Objects.equals(this.options, that.options) &&
          Objects.equals(this.templateName, that.templateName) &&
          Objects.equals(this.domains, that.domains) &&
          Objects.equals(this.edgeCacheDNS, that.edgeCacheDNS) &&
          Objects.equals(this.edgeCacheDomains, that.edgeCacheDomains);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, edgeCacheDomains);
  }
}

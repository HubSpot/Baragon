package com.hubspot.baragon.models;

import java.nio.charset.StandardCharsets;
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

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

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

  private final boolean preResolveUpstreamDNS;

  private final String serviceIdHash;

  public BaragonService(@JsonProperty("serviceId") String serviceId,
                        @JsonProperty("owners") Collection<String> owners,
                        @JsonProperty("serviceBasePath") String serviceBasePath,
                        @JsonProperty("additionalPaths") List<String> additionalPaths,
                        @JsonProperty("loadBalancerGroups") Set<String> loadBalancerGroups,
                        @JsonProperty("options") Map<String, Object> options,
                        @JsonProperty("templateName") Optional<String> templateName,
                        @JsonProperty("domains") Set<String> domains,
                        @JsonProperty("edgeCacheDNS") Optional<String> edgeCacheDNS,
                        @JsonProperty("edgeCacheDomains") Set<String> edgeCacheDomains,
                        @JsonProperty("preResolveUpstreamDNS") boolean preResolveUpstreamDNS) {
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
    this.preResolveUpstreamDNS = preResolveUpstreamDNS;
    this.serviceIdHash = DigestUtils.sha256Hex(serviceId);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options,
                        Optional<String> templateName, Set<String> domains, Optional<String> edgeCacheDNS, Set<String> edgeCacheDomains) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, edgeCacheDomains, false);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options,
                        Optional<String> templateName, Set<String> domains, Optional<String> edgeCacheDNS) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, Collections.emptySet(), false);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options,
                        Optional<String> templateName, Set<String> domains, Set<String> edgeCacheDomains) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, Optional.absent(), edgeCacheDomains, false);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName, Set<String> domains) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, Optional.absent(), Collections.emptySet(), false);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, List<String> additionalPaths, Set<String> loadBalancerGroups, Map<String, Object> options, Optional<String> templateName) {
    this(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, Collections.<String>emptySet(), Optional.absent(), Collections.emptySet(), false);
  }

  public BaragonService(String serviceId, Collection<String> owners, String serviceBasePath, Set<String> loadBalancerGroups, Map<String, Object> options) {
    this(serviceId, owners, serviceBasePath, Collections.<String>emptyList(), loadBalancerGroups, options, Optional.<String>absent(), Collections.<String>emptySet(), Optional.absent(), Collections.emptySet(), false);
  }

  public BaragonService withUpdatedGroups(BaragonGroupAlias updatedFromAlias) {
    return new BaragonServiceBuilder().setServiceId(serviceId)
        .setOwners(owners)
        .setServiceBasePath(serviceBasePath)
        .setAdditionalPaths(additionalPaths)
        .setLoadBalancerGroups(updatedFromAlias.getGroups())
        .setOptions(options)
        .setTemplateName(templateName)
        .setDomains(updatedFromAlias.getDomains())
        .setEdgeCacheDomains(updatedFromAlias.getEdgeCacheDomains())
        .setPreResolveUpstreamDNS(preResolveUpstreamDNS)
        .build();
  }

  public BaragonService withDomains(Set<String> domains) {
    return new BaragonServiceBuilder().setServiceId(serviceId)
        .setOwners(owners)
        .setServiceBasePath(serviceBasePath)
        .setAdditionalPaths(additionalPaths)
        .setLoadBalancerGroups(loadBalancerGroups)
        .setOptions(options)
        .setTemplateName(templateName)
        .setDomains(domains)
        .setEdgeCacheDomains(edgeCacheDomains)
        .setPreResolveUpstreamDNS(preResolveUpstreamDNS)
        .build();
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

  public boolean isPreResolveUpstreamDNS() {
    return preResolveUpstreamDNS;
  }

  public String getServiceIdHash() {
    if (serviceIdHash == null || serviceIdHash.equals("")){
      return DigestUtils.sha256Hex(serviceId);
    }
    return serviceIdHash;
  }

  @JsonIgnore
  public List<String> getAllPaths(Optional<String> defaultDomain) {
    List<String> allPaths = new ArrayList<>();
    for (String path : additionalPaths) {
      if (!domains.isEmpty()) {
        for (String domain : domains) {
          allPaths.add(String.format("%s%s", domain, path));
          if (defaultDomain.isPresent() && domain.equals(defaultDomain.get())) {
            allPaths.add(path); // For the default domain, also add the unqualified path
          }
        }
      } else {
        allPaths.add(path);
      }
    }
    if (!domains.isEmpty()) {
      for (String domain : domains) {
        allPaths.add(String.format("%s%s", domain, serviceBasePath));
        if (defaultDomain.isPresent() && domain.equals(defaultDomain.get())) {
          allPaths.add(serviceBasePath); // For the default domain, also add the unqualified path
        }
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
        ", preResolveUpstreamDNS=" + preResolveUpstreamDNS +
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
          Objects.equals(this.edgeCacheDomains, that.edgeCacheDomains) &&
          Objects.equals(this.preResolveUpstreamDNS, that.preResolveUpstreamDNS);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, edgeCacheDomains, preResolveUpstreamDNS);
  }
}

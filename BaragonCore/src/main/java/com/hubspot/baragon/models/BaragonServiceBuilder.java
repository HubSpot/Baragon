package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

public class BaragonServiceBuilder {
  private String serviceId;
  private Collection<String> owners;
  private String serviceBasePath;
  private List<String> additionalPaths = Collections.<String>emptyList();
  private Set<String> loadBalancerGroups;
  private Map<String, Object> options;
  private Optional<String> templateName = Optional.<String>absent();
  private Set<String> domains = Collections.<String>emptySet();
  private Optional<String> edgeCacheDNS = Optional.absent();
  private Set<String> edgeCacheDomains = Collections.emptySet();
  private boolean preResolveUpstreamDNS = false;

  public BaragonServiceBuilder setServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  public BaragonServiceBuilder setOwners(Collection<String> owners) {
    this.owners = owners;
    return this;
  }

  public BaragonServiceBuilder setServiceBasePath(String serviceBasePath) {
    this.serviceBasePath = serviceBasePath;
    return this;
  }

  public BaragonServiceBuilder setAdditionalPaths(List<String> additionalPaths) {
    this.additionalPaths = additionalPaths;
    return this;
  }

  public BaragonServiceBuilder setLoadBalancerGroups(Set<String> loadBalancerGroups) {
    this.loadBalancerGroups = loadBalancerGroups;
    return this;
  }

  public BaragonServiceBuilder setOptions(Map<String, Object> options) {
    this.options = options;
    return this;
  }

  public BaragonServiceBuilder setTemplateName(Optional<String> templateName) {
    this.templateName = templateName;
    return this;
  }

  public BaragonServiceBuilder setDomains(Set<String> domains) {
    this.domains = domains;
    return this;
  }

  public BaragonServiceBuilder setEdgeCacheDNS(Optional<String> edgeCacheDNS) {
    this.edgeCacheDNS = edgeCacheDNS;
    return this;
  }

  public BaragonServiceBuilder setEdgeCacheDomains(Set<String> edgeCacheDomains) {
    this.edgeCacheDomains = edgeCacheDomains;
    return this;
  }

  public BaragonServiceBuilder setPreResolveUpstreamDNS(boolean preResolveUpstreamDNS) {
    this.preResolveUpstreamDNS = preResolveUpstreamDNS;
    return this;
  }

  public BaragonService build() {
    return new BaragonService(serviceId, owners, serviceBasePath, additionalPaths, loadBalancerGroups, options, templateName, domains, edgeCacheDNS, edgeCacheDomains, preResolveUpstreamDNS);
  }
}

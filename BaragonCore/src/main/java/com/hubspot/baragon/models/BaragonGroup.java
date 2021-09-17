package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonGroup {
  private final String name;

  @Deprecated
  private Optional<String> domain;

  private Set<TrafficSource> trafficSources;

  @Deprecated
  private Set<String> sources;

  private Optional<String> defaultDomain;
  private Set<String> domains;
  private Map<String, Set<String>> domainAliases;
  private Integer minHealthyAgents;

  @JsonCreator
  public BaragonGroup(
    @JsonProperty("name") String name,
    @JsonProperty("domain") Optional<String> domain,
    @JsonProperty("trafficSources") Set<TrafficSource> trafficSources,
    @JsonProperty("sources") Set<String> sources,
    @JsonProperty("defaultDomain") Optional<String> defaultDomain,
    @JsonProperty("domains") Set<String> domains,
    @JsonProperty("domainAliases") Map<String, Set<String>> domainAliases,
    @JsonProperty(value = "minHealthyAgents", defaultValue = "1") Integer minHealthyAgents
  ) {
    this.name = name;
    this.domain = domain;
    this.defaultDomain = defaultDomain;
    this.domains = MoreObjects.firstNonNull(domains, Collections.emptySet());
    this.domainAliases = MoreObjects.firstNonNull(domainAliases, Collections.emptyMap());
    this.sources = Collections.emptySet();
    this.minHealthyAgents = minHealthyAgents;

    if (trafficSources == null && sources != null) {
      this.trafficSources =
        sources
          .stream()
          .map(
            source ->
              new TrafficSource(source, TrafficSourceType.CLASSIC, RegisterBy.INSTANCE_ID)
          )
          .collect(Collectors.toSet());
    } else {
      this.trafficSources =
        trafficSources != null ? trafficSources : Collections.emptySet();
    }
  }

  public String getName() {
    return name;
  }

  @Deprecated
  public Optional<String> getDomain() {
    return getDefaultDomain();
  }

  @Deprecated
  public void setDomain(Optional<String> domain) {
    this.domain = domain;
  }

  @Deprecated
  public Set<String> getSources() {
    return Collections.emptySet();
  }

  @Deprecated
  public void setSources(Set<String> sources) {}

  public Set<TrafficSource> getTrafficSources() {
    return trafficSources;
  }

  public void setTrafficSources(Set<TrafficSource> sources) {
    this.trafficSources = sources;
  }

  public void removeTrafficSource(TrafficSource trafficSource) {
    this.trafficSources.remove(trafficSource);
  }

  public void addTrafficSource(TrafficSource trafficSource) {
    this.trafficSources.add(trafficSource);
  }

  public Optional<String> getDefaultDomain() {
    return defaultDomain.or(domain);
  }

  public void setDefaultDomain(Optional<String> defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  public Set<String> getDomains() {
    return domains;
  }

  public void setDomains(Set<String> domains) {
    this.domains = domains;
  }

  public Map<String, Set<String>> getDomainAliases() {
    return domainAliases;
  }

  public void setDomainAliases(Map<String, Set<String>> domainAliases) {
    this.domainAliases = domainAliases;
  }

  public void setMinHealthyAgents(Integer minHealthyAgents) {
    this.minHealthyAgents = minHealthyAgents;
  }

  public Integer getMinHealthyAgents() {
    return this.minHealthyAgents == null ? Integer.valueOf(1) : this.minHealthyAgents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaragonGroup that = (BaragonGroup) o;
    return (
      Objects.equals(name, that.name) &&
      Objects.equals(domain, that.domain) &&
      Objects.equals(trafficSources, that.trafficSources) &&
      Objects.equals(sources, that.sources) &&
      Objects.equals(defaultDomain, that.defaultDomain) &&
      Objects.equals(domains, that.domains) &&
      Objects.equals(minHealthyAgents, that.minHealthyAgents) &&
      Objects.equals(domainAliases, that.domainAliases)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      name,
      domain,
      trafficSources,
      sources,
      defaultDomain,
      domains,
      domainAliases,
      minHealthyAgents
    );
  }

  @Override
  public String toString() {
    return (
      "BaragonGroup{" +
      "name='" +
      name +
      '\'' +
      ", domain=" +
      domain +
      ", trafficSources=" +
      trafficSources +
      ", sources=" +
      sources +
      ", defaultDomain=" +
      defaultDomain +
      ", domains=" +
      domains +
      ", domainAliases=" +
      domainAliases +
      ", minHealthyAgents=" +
      getMinHealthyAgents() +
      '}'
    );
  }
}

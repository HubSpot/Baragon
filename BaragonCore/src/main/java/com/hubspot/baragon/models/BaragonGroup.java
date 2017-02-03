package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonGroup {
  private final String name;
  @Deprecated
  private Optional<String> domain;
  private Set<TrafficSource> trafficSources;
  @Deprecated
  private Set<String> sources;
  private Optional<String> defaultDomain;
  private Set<String> domains;

  @JsonCreator
  public BaragonGroup(@JsonProperty("name") String name,
                      @JsonProperty("domain") Optional<String> domain,
                      @JsonProperty("trafficSources") Set<TrafficSource> trafficSources,
                      @JsonProperty("sources") Set<String> sources,
                      @JsonProperty("defaultDomain") Optional<String> defaultDomain,
                      @JsonProperty("domains") Set<String> domains) {
    this.name = name;
    this.domain = domain;
    this.defaultDomain = defaultDomain;
    this.domains = MoreObjects.firstNonNull(domains, Collections.<String>emptySet());

    if (sources == null) {
      setTrafficSources(MoreObjects.firstNonNull(trafficSources, Collections.<TrafficSource>emptySet()));
    } else if (trafficSources == null) {
      setSources(sources);
    } else {
      setTrafficSources(trafficSources);
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
    return this.sources;
  }

  @Deprecated
  public void setSources(Set<String> sources) {
    Set<TrafficSource> trafficSources = new HashSet<>();
    for (String source : sources) {
      trafficSources.add(new TrafficSource(source, TrafficSourceType.CLASSIC));
    }

    this.setTrafficSources(trafficSources);
  }

  public Set<TrafficSource> getTrafficSources() {
    return trafficSources;
  }

  public void setTrafficSources(Set<TrafficSource> sources) {
    this.trafficSources = sources;

    Set<String> classicSources = new HashSet<>();
    for (TrafficSource source : sources) {
      if (source.getType() == TrafficSourceType.CLASSIC) {
        classicSources.add(source.getName());
      }
    }

    this.sources = classicSources;
  }

  public void removeTrafficSource(TrafficSource trafficSource) {
    this.trafficSources.remove(trafficSource);
    if (trafficSource.getType() == TrafficSourceType.CLASSIC) {
      sources.remove(trafficSource.getName());
    }
  }

  public void addTrafficSource(TrafficSource trafficSource) {
    this.trafficSources.add(trafficSource);
    if (trafficSource.getType() == TrafficSourceType.CLASSIC) {
      this.sources.add(trafficSource.getName());
    }
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BaragonGroup) {
      final BaragonGroup that = (BaragonGroup) obj;
      return Objects.equals(this.name, that.name) &&
          Objects.equals(this.domain, that.domain) &&
          Objects.equals(this.trafficSources, that.trafficSources) &&
          Objects.equals(this.sources, that.sources) &&
          Objects.equals(this.defaultDomain, that.defaultDomain) &&
          Objects.equals(this.domains, that.domains);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, domain, trafficSources, sources, defaultDomain, domains);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("domain", domain)
        .add("trafficSources", trafficSources)
        .add("sources", sources)
        .add("defaultDomain", defaultDomain)
        .add("domains", domains)
        .toString();
  }
}

package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonGroup {
  private final String name;
  @Deprecated
  private Optional<String> domain;
  private Set<TrafficSource> sources;
  private Optional<String> defaultDomain;
  private Set<String> domains;

  @JsonCreator
  public BaragonGroup(@JsonProperty("name") String name,
                      @JsonProperty("domain") Optional<String> domain,
                      @JsonProperty("sources") Set<TrafficSource> sources,
                      @JsonProperty("defaultDomain") Optional<String> defaultDomain,
                      @JsonProperty("domains") Set<String> domains) {
    this.name = name;
    this.domain = domain;
    this.sources = Objects.firstNonNull(sources, Collections.<TrafficSource>emptySet());
    this.defaultDomain = defaultDomain;
    this.domains = Objects.firstNonNull(domains, Collections.<String>emptySet());
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

  public Set<TrafficSource> getSources() {
    return sources;
  }

  public void setSources(Set<TrafficSource> sources) {
    this.sources = sources;
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonGroup that = (BaragonGroup) o;

    if (!name.equals(that.name)) {
      return false;
    }
    if (!domain.equals(that.domain)) {
      return false;
    }
    if (!sources.equals(that.sources)) {
      return false;
    }
    if (!defaultDomain.equals(that.defaultDomain)) {
      return false;
    }
    if (!domains.equals(that.domains)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + domain.hashCode();
    result = 31 * result + sources.hashCode();
    result = 31 * result + defaultDomain.hashCode();
    result = 31 * result + domains.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BaragonGroup [" +
      "name=" + name +
      ", domain=" + domain +
      ", sources=" + sources +
      ", defaultDomain=" + defaultDomain +
      ", domains=" + domains +
      ']';
  }
}

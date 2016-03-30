package com.hubspot.baragon.models;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonGroup {
  private final String name;
  private Optional<String> defaultDomain;
  private Set<String> sources;
  private List<String> domainsServed;

  @JsonCreator
  public BaragonGroup(@JsonProperty("name") String name,
                      @JsonProperty("defaultDomain") Optional<String> defaultDomain,
                      @JsonProperty("sources") Set<String> sources,
                      @JsonProperty("domainsServed") List<String> domainsServed) {
    this.name = name;
    this.defaultDomain = defaultDomain;
    this.sources = sources;
    this.domainsServed = domainsServed;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getDefaultDomain() {
    return defaultDomain;
  }

  public void setDefaultDomain(Optional<String> defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  @Deprecated
  @JsonIgnore
  public Optional<String> getDomain() {
    return defaultDomain;
  }

  @Deprecated
  @JsonSetter("domain")
  public void setDomain(Optional<String> domain) {
    if (!this.defaultDomain.isPresent()) {
      this.defaultDomain = domain;
    }
  }

  public Set<String> getSources() {
    return sources;
  }

  public void addSource(String source) {
    this.sources.add(source);
  }

  public void removeSource(String source) {
    this.sources.remove(source);
  }

  public List<String> getDomainsServed() {
    return domainsServed;
  }

  public void setDomainsServed(List<String> domainsServed) {
    this.domainsServed = domainsServed;
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
    if (!defaultDomain.equals(that.defaultDomain)) {
      return false;
    }
    if (!sources.equals(that.sources)) {
      return false;
    }
    if (!domainsServed.equals(that.domainsServed)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + defaultDomain.hashCode();
    result = 31 * result + sources.hashCode();
    result = 31 * result + domainsServed.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "BaragonGroup [" +
      "name=" + name +
      ", defaultDomain=" + defaultDomain +
      ", sources=" + sources +
      ", domainsServed=" + domainsServed +
      ']';
  }
}

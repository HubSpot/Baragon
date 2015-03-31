package com.hubspot.baragon.models;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonGroup {
  private final String name;
  private Optional<String> domain;
  private Set<String> sources;

  @JsonCreator
  public BaragonGroup(@JsonProperty("name") String name,
                      @JsonProperty("domain") Optional<String> domain,
                      @JsonProperty("sources") Set<String> sources) {
    this.name = name;
    this.domain = domain;
    this.sources = sources;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getDomain() {
    return domain;
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

  public void setDomain(Optional<String> domain) {
    this.domain = domain;
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
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + domain.hashCode();
    result = 31 * result + sources.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ElbGroup [" +
      "name=" + name +
      ", domain=" + domain +
      ", sources" + sources +
      ']';
  }
}

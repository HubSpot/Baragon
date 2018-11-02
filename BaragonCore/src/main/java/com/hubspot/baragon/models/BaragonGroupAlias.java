package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaragonGroupAlias {
  private final Set<String> groups;
  private final Set<String> domains;
  private final Set<String> edgeCacheDomains;

  @JsonCreator

  public BaragonGroupAlias(@JsonProperty("groups") Set<String> groups,
                           @JsonProperty("domains") Set<String> domains,
                           @JsonProperty("edgeCacheDomains") Set<String> edgeCacheDomains) {
    this.groups = groups == null ? Collections.emptySet() : groups;
    this.domains = domains == null ? Collections.emptySet() : domains;
    this.edgeCacheDomains = edgeCacheDomains == null ? Collections.emptySet() : edgeCacheDomains;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public Set<String> getDomains() {
    return domains;
  }

  public Set<String> getEdgeCacheDomains() {
    return edgeCacheDomains;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonGroupAlias that = (BaragonGroupAlias) o;

    if (groups != null ? !groups.equals(that.groups) : that.groups != null) {
      return false;
    }
    if (domains != null ? !domains.equals(that.domains) : that.domains != null) {
      return false;
    }
    return edgeCacheDomains != null ? edgeCacheDomains.equals(that.edgeCacheDomains) : that.edgeCacheDomains == null;
  }

  @Override
  public int hashCode() {
    int result = groups != null ? groups.hashCode() : 0;
    result = 31 * result + (domains != null ? domains.hashCode() : 0);
    result = 31 * result + (edgeCacheDomains != null ? edgeCacheDomains.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "BaragonGroupAlias{" +
        "groups='" + groups + '\'' +
        ", domains=" + domains +
        ", edgeCacheDomains=" + edgeCacheDomains +
        '}';
  }
}

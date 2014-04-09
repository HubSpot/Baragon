package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {
  private static final Map<String, Object> BLANK_OPTIONS = Collections.emptyMap();

  private final String id;
  private final Collection<String> owners;
  private final String route;
  private final List<String> lbs;
  private final Map<String, Object> options;
  
  public Service(@JsonProperty("id") String id,
                 @JsonProperty("owners") Collection<String> owners, @JsonProperty("route") String route,
                 @JsonProperty("lbs") List<String> lbs,
                 @JsonProperty("options") Map<String, Object> options) {
    this.id = id;
    this.owners = owners;
    this.route = route;
    this.lbs = lbs;
    this.options = Objects.firstNonNull(options, BLANK_OPTIONS);

    // TODO: bring back rewriteAppRootToo (sorry Ian)
  }

  public String getId() {
    return id;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public String getRoute() {
    return route;
  }

  public List<String> getLbs() {
    return lbs;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Service.class)
        .add("id", id)
        .add("owners", owners)
        .add("route", route)
        .add("lbs", lbs)
        .add("options", options)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, owners, route, lbs, options);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof Service) {
      return Objects.equal(id, ((Service)that).getId())
          && Objects.equal(owners, ((Service)that).getOwners())
          && Objects.equal(route, ((Service)that).getRoute())
          && Objects.equal(lbs, ((Service)that).getLbs())
          && Objects.equal(options, ((Service)that).getOptions());
    }

    return false;
  }
}

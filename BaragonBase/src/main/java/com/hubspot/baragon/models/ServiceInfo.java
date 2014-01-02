package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceInfo {
  private static final Map<String, Object> BLANK_OPTIONS = Collections.emptyMap();
  private static final Log LOG = LogFactory.getLog(ServiceInfo.class);
  private final String name;
  private final String id;
  private final String contactEmail;
  private final String route;
  private final String healthCheck;
  private final List<String> lbs;
  private final String rewriteAppRootTo;
  private final Map<String, Object> options;
  
  public ServiceInfo(@JsonProperty("name") String name, @JsonProperty("id") String id,
                     @JsonProperty("contactEmail") String contactEmail, @JsonProperty("route") String route,
                     @JsonProperty("healthCheck") String healthCheck,
                     @JsonProperty("lbs") List<String> lbs,
                     @JsonProperty("rewriteAppRootTo") String rewriteAppRootTo,
                     @JsonProperty("options") Map<String, Object> options) {
    this.name = name;
    this.id = id;
    this.contactEmail = contactEmail;
    this.route = route;
    this.healthCheck = healthCheck;
    this.lbs = lbs;
    this.options = Objects.firstNonNull(options, BLANK_OPTIONS);
    if (rewriteAppRootTo != null) {
      if (isAbsoluteURI(route) && ! isAbsoluteURI(rewriteAppRootTo)) {
        LOG.error(String.format("Provided appRoot %s is absolute, and rewriteAppRootTo %s is not.  This will result in a rewrite of %sresource to %sresource",
                                    route, rewriteAppRootTo, route, rewriteAppRootTo));
        this.rewriteAppRootTo = null;
      } else if (! isAbsoluteURI(route) && isAbsoluteURI(rewriteAppRootTo)) {
        LOG.error(String.format("Provided appRoot %s is not absolute, and rewriteAppRootTo %s is.  This will result in a rewrite of %sresource to %s resource",
                                    route, rewriteAppRootTo, route, rewriteAppRootTo));
        this.rewriteAppRootTo = null;
      } else {
        this.rewriteAppRootTo = rewriteAppRootTo;
      }
    } else {
      this.rewriteAppRootTo = null;
    } 
  }

  private boolean isAbsoluteURI(String uri) {
    if (uri.substring(uri.length() - 1).equals("/")) {
      return true;
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getRoute() {
    return route;
  }

  public String getHealthCheck() {
    return healthCheck;
  }

  public List<String> getLbs() {
    return lbs;
  }

  public String getRewriteAppRootTo() {
    return rewriteAppRootTo;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(ServiceInfo.class)
        .add("name", name)
        .add("id", id)
        .add("contactEmail", contactEmail)
        .add("route", route)
        .add("healthCheck", healthCheck)
        .add("lbs", lbs)
        .add("rewriteAppRootTo", rewriteAppRootTo)
        .add("options", options)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, id, contactEmail, route, healthCheck, lbs, rewriteAppRootTo, options);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof ServiceInfo) {
      return Objects.equal(name, ((ServiceInfo)that).getName())
          && Objects.equal(id, ((ServiceInfo)that).getId())
          && Objects.equal(contactEmail, ((ServiceInfo)that).getContactEmail())
          && Objects.equal(route, ((ServiceInfo)that).getRoute())
          && Objects.equal(healthCheck, ((ServiceInfo)that).getHealthCheck())
          && Objects.equal(lbs, ((ServiceInfo)that).getLbs())
          && Objects.equal(rewriteAppRootTo, ((ServiceInfo)that).getRewriteAppRootTo())
          && Objects.equal(options, ((ServiceInfo)that).getOptions());
    }

    return false;
  }
}

package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;

public class ServiceInfo {
  private static final Log LOG = LogFactory.getLog(ServiceInfo.class);
  private final String name;
  private final String id;
  private final String contactEmail;
  private final String route;
  private final List<String> extraConfigs;
  private final String healthCheck;
  private final List<String> lbs;
  private final String rewriteAppRootTo;
  
  public ServiceInfo(@JsonProperty("name") String name, @JsonProperty("id") String id,
                     @JsonProperty("contactEmail") String contactEmail, @JsonProperty("route") String route,
                     @JsonProperty("extraConfigs") List<String> extraConfigs,
                     @JsonProperty("healthCheck") String healthCheck,
                     @JsonProperty("lbs") List<String> lbs,
                     @JsonProperty("rewriteAppRootTo") String rewriteAppRootTo) {
    this.name = name;
    this.id = id;
    this.contactEmail = contactEmail;
    this.route = route;
    this.extraConfigs = Objects.firstNonNull(extraConfigs, Collections.<String>emptyList());
    this.healthCheck = healthCheck;
    this.lbs = lbs;
    if (rewriteAppRootTo != null) {
      if (isAbsoluteURI(route) && ! isAbsoluteURI(rewriteAppRootTo)) {
        LOG.error(String.format("Provided appRoot %s is absolute, and rewriteAppRootTo %s is not.  This will result in a rewrite of %sresource to %sresource",
                                    route, rewriteAppRootTo, route, rewriteAppRootTo));
      } else if (! isAbsoluteURI(route) && isAbsoluteURI(rewriteAppRootTo)) {
        LOG.error(String.format("Provided appRoot %s is not absolute, and rewriteAppRootTo %s is.  This will result in a rewrite of %sresource to %s resource",
                                    route, rewriteAppRootTo, route, rewriteAppRootTo));
      } else {
        this.rewriteAppRootTo = rewriteAppRootTo;
      }
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

  public List<String> getExtraConfigs() {
    return extraConfigs;
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
}

package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

public class ServiceInfo {
  private final String name;
  private final String id;
  private final String contactEmail;
  private final String route;
  private final List<String> extraConfigs;
  private final String healthCheck;
  private final List<String> lbs;
  
  public ServiceInfo(@JsonProperty("name") String name, @JsonProperty("id") String id,
                     @JsonProperty("contactEmail") String contactEmail, @JsonProperty("route") String route,
                     @JsonProperty("extraConfigs") List<String> extraConfigs,
                     @JsonProperty("healthCheck") String healthCheck,
                     @JsonProperty("lbs") List<String> lbs) {
    this.name = name;
    this.id = id;
    this.contactEmail = contactEmail;
    this.route = route;
    this.extraConfigs = extraConfigs;
    this.healthCheck = healthCheck;
    this.lbs = lbs;
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
}
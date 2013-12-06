package com.hubspot.baragon.exceptions;

import com.hubspot.baragon.models.ServiceInfo;

import java.util.Collection;

public class MissingLoadBalancersException extends RuntimeException {
  private final ServiceInfo serviceInfo;
  private final Collection<String> missingLoadBalancers;

  public MissingLoadBalancersException(ServiceInfo serviceInfo, Collection<String> missingLoadBalancers) {
    this.serviceInfo = serviceInfo;
    this.missingLoadBalancers = missingLoadBalancers;
  }

  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  public Collection<String> getMissingLoadBalancers() {
    return missingLoadBalancers;
  }
}

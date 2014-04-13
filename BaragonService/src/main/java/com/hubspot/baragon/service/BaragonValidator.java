package com.hubspot.baragon.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.BaragonRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collection;

public class BaragonValidator {
  private BaragonValidator() {

  }

  public static void validateRequest(BaragonRequest request) {
    final Collection<String> errors = Lists.newArrayList();

    if (Strings.isNullOrEmpty(request.getLoadBalancerRequestId())) {
      errors.add("loadBalancerRequestId cannot be null or empty");
    }

    if (request.getLoadBalancerService() == null) {
      errors.add("loadBalancerService cannot be null");
    } else {
      final Service service = request.getLoadBalancerService();

      if (Strings.isNullOrEmpty(service.getServiceId())) {
        errors.add("loadBalancerService.serviceId cannot be null or empty");
      }

      if (service.getLoadBalancerGroups() == null || service.getLoadBalancerGroups().isEmpty()) {
        errors.add("loadBalancerService.loadBalancerGroups cannot be null or empty");
      }

      if (Strings.isNullOrEmpty(service.getLoadBalancerBaseUri())) {
        errors.add("loadBalancerService.loadBalancerBaseUri cannot be null or empty");
      }
    }

    if (request.getAddUpstreams() == null) {
      errors.add("addUpstreams cannot be null");
    }

    if (request.getRemoveUpstreams() == null) {
      errors.add("removeUpstreams cannot be null");
    }

    if (!errors.isEmpty()) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
    }
  }
}

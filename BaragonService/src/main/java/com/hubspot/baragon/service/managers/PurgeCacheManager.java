package com.hubspot.baragon.service.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.service.config.BaragonConfiguration;

public class PurgeCacheManager {

  private static final Logger LOG = LoggerFactory.getLogger(PurgeCacheManager.class);

  private final BaragonConfiguration baragonConfiguration;

  @Inject
  public PurgeCacheManager(BaragonConfiguration baragonConfiguration){
    this.baragonConfiguration = baragonConfiguration;
  }

  public BaragonRequest updateForPurgeCache(BaragonRequest request){
    if (request.getAction().isPresent() && request.getAction().get().equals(RequestAction.UPDATE)){
      if (serviceShouldPurgeCache(request.getLoadBalancerService())){
        LOG.info("serviceId={} to be updated to set purgeCache=true", request.getLoadBalancerService().getServiceId());
        return request.withUpdatedPurgeCache(true);
      }
    }
    return request;
  }

  public boolean serviceShouldPurgeCache(BaragonService service){
    // 1. if the serviceId is on the exclude list, return false
    if (baragonConfiguration.getPurgeCacheConfiguration().getExcludedServiceIds().contains(
        Optional.of(service.getServiceId()).or("")
    )){
      return false;
    }
    // 2. if the service's templateName is not in the enabledTemplates list, return false, otherwise return true
    return baragonConfiguration.getPurgeCacheConfiguration().getEnabledTemplates().contains(service.getTemplateName().or(""));
  }

}

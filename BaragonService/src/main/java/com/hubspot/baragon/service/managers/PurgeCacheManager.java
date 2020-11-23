package com.hubspot.baragon.service.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
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
      if (baragonConfiguration.getPurgeCacheConfiguration().serviceShouldPurgeCache(request.getLoadBalancerService())){
        LOG.info("serviceId={} with RequestAction.UPDATE to be rewritten to UPDATE_AND_PURGE_CACHE", request.getLoadBalancerService().getServiceId());
        request.setAction(Optional.of(RequestAction.UPDATE_AND_PURGE_CACHE));
      }
    }
    return request;
  }

}

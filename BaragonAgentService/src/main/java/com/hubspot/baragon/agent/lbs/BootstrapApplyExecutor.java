package com.hubspot.baragon.agent.lbs;

import com.google.common.base.Optional;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.utils.JavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class BootstrapApplyExecutor implements Callable<BaragonService> {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapApplyExecutor.class);

  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonServiceState serviceState;
  private final long now;

  public BootstrapApplyExecutor(LoadBalancerConfiguration loadBalancerConfiguration,
                                FilesystemConfigHelper configHelper,
                                BaragonServiceState serviceState,
                                long now) {

    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.serviceState = serviceState;
    this.now = now;
  }

  @Override
  public BaragonService call() {
    if (!(serviceState.getService().getLoadBalancerGroups() == null) && serviceState.getService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
      try {
        configHelper.apply(new ServiceContext(serviceState.getService(), serviceState.getUpstreams(), now, true), Optional.<BaragonService>absent(), false);
      } catch (Exception e) {
        LOG.error(String.format("Caught exception while applying %s", serviceState.getService().getServiceId()), e);
      }
    }
    return serviceState.getService();
  }
}

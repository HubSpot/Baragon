package com.hubspot.baragon.agent.lbs;

import java.util.Collection;
import java.util.concurrent.Callable;

import com.google.common.base.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.ServiceContext;

public class BootstrapFileChecker implements Callable<Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>>> {
  private static final Logger LOG = LoggerFactory.getLogger(BootstrapFileChecker.class);

  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final FilesystemConfigHelper configHelper;
  private final BaragonServiceState serviceState;
  private final long now;

  public BootstrapFileChecker(LoadBalancerConfiguration loadBalancerConfiguration,
                              FilesystemConfigHelper configHelper,
                              BaragonServiceState serviceState,
                              long now) {

    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.serviceState = serviceState;
    this.now = now;
  }

  @Override
  public Optional<Pair<ServiceContext, Collection<BaragonConfigFile>>> call() {
    if (!(serviceState.getService().getLoadBalancerGroups() == null) && serviceState.getService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
      try {
        ServiceContext context = new ServiceContext(serviceState.getService(), serviceState.getUpstreams(), now, true);
        Optional<Collection<BaragonConfigFile>> maybeConfigsToApply = configHelper.configsToApply(context);
        if (maybeConfigsToApply.isPresent()) {
          Pair<ServiceContext, Collection<BaragonConfigFile>> configMap = new ImmutablePair<>(context, maybeConfigsToApply.get());
          return Optional.of(configMap);
        }
      } catch (Exception e) {
        LOG.error(String.format("Caught exception while finding config for %s", serviceState.getService()), e);
      }
    }
    LOG.info(String.format("Don't need to apply %s", serviceState.getService().getServiceId()));
    return Optional.absent();
  }
}

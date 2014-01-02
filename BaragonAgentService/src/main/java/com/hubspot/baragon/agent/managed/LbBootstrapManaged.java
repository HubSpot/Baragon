package com.hubspot.baragon.agent.managed;

import com.google.common.base.Optional;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.utils.SnapshotUtils;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.lbs.LbAdapter;
import com.hubspot.baragon.lbs.LbConfigHelper;

public class LbBootstrapManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(LbBootstrapManaged.class);
  
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final LbConfigHelper configHelper;
  private final LbAdapter adapter;
  private final BaragonDataStore datastore;
  private final SnapshotUtils snapshotUtils;
  
  @Inject
  public LbBootstrapManaged(BaragonDataStore datastore, LoadBalancerConfiguration loadBalancerConfiguration,
                            LbConfigHelper configHelper, LbAdapter adapter, SnapshotUtils snapshotUtils) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.adapter = adapter;
    this.datastore = datastore;
    this.snapshotUtils = snapshotUtils;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Loading initial LB state from datastore...");
    
    boolean appliedConfigs = false;

    for (String serviceName : datastore.getActiveServices()) {
      Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);

      if (!maybeServiceInfo.isPresent()) {
        LOG.warn(String.format("%s is listed as an active service, but no service info exists!", serviceName));
        continue;
      }

      if (maybeServiceInfo.get().getLbs() == null || !maybeServiceInfo.get().getLbs().contains(loadBalancerConfiguration.getName())) {
        continue;
      }

      try {
        configHelper.apply(snapshotUtils.buildSnapshot(maybeServiceInfo.get()));
        appliedConfigs = true;
      } catch (Exception e) {
        LOG.error("Exception while trying to load active deploys:", e);
        if (!loadBalancerConfiguration.getAlwaysApplyConfigs()) {
          appliedConfigs = false;
          break;
        }
      }
    }
    
    if (appliedConfigs) {
      LOG.info("We've applied new configs. Checking & reloading...");
      adapter.checkConfigs();
      adapter.reloadConfigs();
    }
  }

  @Override
  public void stop() throws Exception {
    // nothing to see here, folks.
  }

}

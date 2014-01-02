package com.hubspot.baragon.lbs;

import com.google.common.base.Throwables;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.models.ServiceSnapshot;
import com.hubspot.baragon.utils.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.Collection;

public abstract class LbConfigHelper {
  protected final LbConfigGenerator configGenerator;
  protected final LbAdapter adapter;
  protected final LoadBalancerConfiguration loadBalancerConfiguration;
  
  public LbConfigHelper(LbAdapter adapter, LbConfigGenerator configGenerator,
                        LoadBalancerConfiguration loadBalancerConfiguration) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }
  
  private static final Log LOG = LogFactory.getLog(LbConfigHelper.class);
  
  protected abstract Collection<LbConfigFile> readConfigs(ServiceInfo serviceInfo);
  protected abstract void writeConfigs(Collection<LbConfigFile> files);
  public abstract void backupConfigs(ServiceInfo serviceInfo);
  public abstract void restoreConfigs(ServiceInfo serviceInfo);
  
  public void remove(ServiceInfo serviceInfo) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceInfo)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException("Failed to remove " + filename + " for " + serviceInfo.getName());
      }
    }
  }
  
  public void apply(ServiceSnapshot snapshot) {

    LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "Going to apply %s", LogUtils.COMMA_JOINER.join(snapshot.getHealthyUpstreams()));
    
    // backup old configs
    backupConfigs(snapshot.getServiceInfo());
    
    // write & check the configs
    try {
      writeConfigs(configGenerator.generateConfigsForProject(snapshot));
      adapter.checkConfigs();
    } catch (Exception e) {
      if (loadBalancerConfiguration.getRollbackConfigsIfInvalid()) {
        LOG.error("Caught exception while writing configs for " + snapshot.getServiceInfo().getName() + ", reverting to backups!", e);

        restoreConfigs(snapshot.getServiceInfo());
      }
      
      throw Throwables.propagate(e);
    }
    
    // load the new configs
    adapter.reloadConfigs();
  }
}

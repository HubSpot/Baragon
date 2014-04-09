package com.hubspot.baragon.lbs;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceContext;
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
  
  protected abstract Collection<LbConfigFile> readConfigs(Service service);
  protected abstract void writeConfigs(Collection<LbConfigFile> files);
  public abstract void backupConfigs(Service service);
  public abstract void restoreConfigs(Service service);
  
  public void remove(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException("Failed to remove " + filename + " for " + service.getId());
      }
    }
  }
  
  public void apply(ServiceContext snapshot) {

    LOG.info(String.format("Going to apply %s: %s", snapshot.getService().getId(), Joiner.on(", ").join(snapshot.getUpstreams())));
    
    // backup old configs
    backupConfigs(snapshot.getService());
    
    // write & check the configs
    try {
      writeConfigs(configGenerator.generateConfigsForProject(snapshot));
      adapter.checkConfigs();
    } catch (Exception e) {
      if (loadBalancerConfiguration.getRollbackConfigsIfInvalid()) {
        LOG.error("Caught exception while writing configs for " + snapshot.getService().getId() + ", reverting to backups!", e);

        restoreConfigs(snapshot.getService());
      }
      
      throw Throwables.propagate(e);
    }
    
    // load the new configs
    adapter.reloadConfigs();
  }
}

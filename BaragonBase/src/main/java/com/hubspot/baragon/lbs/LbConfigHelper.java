package com.hubspot.baragon.lbs;

import java.io.File;
import java.util.Collection;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class LbConfigHelper {
  protected final LbConfigGenerator configGenerator;
  protected final LbAdapter adapter;
  
  public LbConfigHelper(LbAdapter adapter, LbConfigGenerator configGenerator) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
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
  
  public void apply(ServiceInfo serviceInfo, Collection<String> upstreams) {

    LOG.info(String.format("Going to apply %s", serviceInfo.getName()));
    LOG.info("    Upstreams: " + Joiner.on(", ").join(upstreams));
    
    // backup old configs
    backupConfigs(serviceInfo);
    
    // write & check the configs
    try {
      writeConfigs(configGenerator.generateConfigsForProject(serviceInfo, upstreams));
      adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error("Caught exception while writing configs for " + serviceInfo.getName() + ", reverting to backups!", e);
      
      restoreConfigs(serviceInfo);
      
      throw Throwables.propagate(e);
    }
    
    // load the new configs
    adapter.reloadConfigs();
  }
}

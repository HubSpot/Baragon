package com.hubspot.baragon.agent.lbs;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.models.LbConfigFile;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Singleton
public class FilesystemConfigHelper {
  private static final Log LOG = LogFactory.getLog(FilesystemConfigHelper.class);

  private final LbConfigGenerator configGenerator;
  private final LocalLbAdapter adapter;
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public FilesystemConfigHelper(LbConfigGenerator configGenerator, LocalLbAdapter adapter, LoadBalancerConfiguration loadBalancerConfiguration) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  public void remove(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException("Failed to remove " + filename + " for " + service.getServiceId());
      }
    }
  }

  public void apply(ServiceContext context) {

    LOG.info(String.format("Going to apply %s: %s", context.getService().getServiceId(), Joiner.on(", ").join(context.getUpstreams())));

    // backup old configs
    backupConfigs(context.getService());

    // write & check the configs
    try {
      writeConfigs(configGenerator.generateConfigsForProject(context));
      adapter.checkConfigs();
    } catch (Exception e) {
      if (loadBalancerConfiguration.getRollbackConfigsIfInvalid()) {
        LOG.error("Caught exception while writing configs for " + context.getService().getServiceId() + ", reverting to backups!", e);

        restoreConfigs(context.getService());
      }

      throw Throwables.propagate(e);
    }

    // load the new configs
    adapter.reloadConfigs();
  }

  private Collection<LbConfigFile> readConfigs(Service service) {
    Collection<LbConfigFile> files = Lists.newLinkedList();
    
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File file = new File(filename);
        final String content = Files.toString(file, Charsets.UTF_8);
        if (file.exists()) {
          files.add(new LbConfigFile(filename, content));
        }
      } catch (Exception e) {
        LOG.error("Failed to back up " + filename, e);
        throw new RuntimeException("Failed backing up " + filename, e);
      }
    }
    
    return files;
  }

  private void writeConfigs(Collection<LbConfigFile> files) {
    for (LbConfigFile file : files) {
      try {
        Files.write(file.getContent().getBytes(), new File(file.getFullPath()));
      } catch (IOException e) {
        LOG.error("Failed writing " + file.getFullPath(), e);
        throw new RuntimeException("Failed writing " + file.getFullPath(), e);
      }
    }
  }

  public void backupConfigs(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename);
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename + ".old");
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error("Failed to backup " + filename, e);
        throw new RuntimeException("Failed to backup " + filename);
      }
    }
  }

  public void restoreConfigs(Service service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename + ".old");
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename);
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error("Failed to restore " + filename, e);
        throw new RuntimeException("Failed to restore " + filename);
      }
    }
  }
}

package com.hubspot.baragon.agent.lbs;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.models.LbConfigFile;
import com.hubspot.baragon.agent.models.ServiceContext;
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

  @Inject
  public FilesystemConfigHelper(LbConfigGenerator configGenerator, LocalLbAdapter adapter) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
  }

  public void remove(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException("Failed to remove " + filename + " for " + serviceId);
      }
    }
  }

  public void apply(ServiceContext context) {

    LOG.info(String.format("Going to apply %s: %s", context.getService().getServiceId(), Joiner.on(", ").join(context.getUpstreams())));
    final boolean newServiceExists = configsExist(context.getService().getServiceId());

    // Backup configs
    if (newServiceExists) {
      backupConfigs(context.getService().getServiceId());
    }

    // Write & check the configs
    try {
      writeConfigs(configGenerator.generateConfigsForProject(context));

      adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error("Caught exception while writing configs for " + context.getService().getServiceId() + ", reverting to backups!", e);

      // Restore configs
      if (newServiceExists) {
        restoreConfigs(context.getService().getServiceId());
      } else {
        remove(context.getService().getServiceId());
      }

      throw Throwables.propagate(e);
    }

    // Load the new configs
    adapter.reloadConfigs();
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

  public void backupConfigs(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
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

  public boolean configsExist(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      if (!new File(filename).exists()) {
        return false;
      }
    }

    return true;
  }

  public void restoreConfigs(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
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

package com.hubspot.baragon.agent.lbs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.models.LbConfigFile;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;
import com.hubspot.baragon.models.ServiceContext;

@Singleton
public class FilesystemConfigHelper {
  public static final String BACKUP_FILENAME_SUFFIX = ".old";

  private static final Logger LOG = LoggerFactory.getLogger(FilesystemConfigHelper.class);

  private final LbConfigGenerator configGenerator;
  private final LocalLbAdapter adapter;

  @Inject
  public FilesystemConfigHelper(LbConfigGenerator configGenerator, LocalLbAdapter adapter) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
  }

  public void remove(String serviceId, boolean reloadConfigs) throws LbAdapterExecuteException, IOException {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException("Failed to remove " + filename + " for " + serviceId);
      }
    }

    if (reloadConfigs) {
      adapter.reloadConfigs();
    }
  }

  public void apply(ServiceContext context, boolean revertOnFailure) throws InvalidConfigException, LbAdapterExecuteException, IOException {
    final String serviceId = context.getService().getServiceId();

    LOG.info(String.format("Going to apply %s: %s", serviceId, Joiner.on(", ").join(context.getUpstreams())));
    final boolean newServiceExists = configsExist(serviceId);

    // Backup configs
    if (newServiceExists && revertOnFailure) {
      backupConfigs(serviceId);
    }

    // Write & check the configs
    try {
      if (context.isPresent()) {
        writeConfigs(configGenerator.generateConfigsForProject(context));
      } else {
        remove(serviceId, false);
      }

      adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error("Caught exception while writing configs for " + serviceId + ", reverting to backups!", e);

      // Restore configs
      if (revertOnFailure) {
        if (newServiceExists) {
          restoreConfigs(serviceId);
        } else {
          remove(serviceId, false);
        }
      }

      throw Throwables.propagate(e);
    }

    // Load the new configs
    adapter.reloadConfigs();

    removeBackupConfigs(serviceId);
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

  private void backupConfigs(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      try {
        File src = new File(filename);
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename + BACKUP_FILENAME_SUFFIX);
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error("Failed to backup " + filename, e);
        throw new RuntimeException("Failed to backup " + filename);
      }
    }
  }

  private void removeBackupConfigs(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      File file = new File(filename + BACKUP_FILENAME_SUFFIX);
      if (!file.exists()) {
        continue;
      }
    }
  }

  private boolean configsExist(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      if (!new File(filename).exists()) {
        return false;
      }
    }

    return true;
  }

  private void restoreConfigs(String serviceId) {
    for (String filename : configGenerator.getConfigPathsForProject(serviceId)) {
      try {
        File src = new File(filename + BACKUP_FILENAME_SUFFIX);
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

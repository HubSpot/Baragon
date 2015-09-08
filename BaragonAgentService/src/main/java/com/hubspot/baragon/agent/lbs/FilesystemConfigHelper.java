package com.hubspot.baragon.agent.lbs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;
import com.hubspot.baragon.exceptions.MissingTemplateException;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public void remove(BaragonService service, boolean reloadConfigs) throws LbAdapterExecuteException, IOException {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException(String.format("Failed to remove %s for %s", filename, service.getServiceId()));
      }
    }

    if (reloadConfigs) {
      adapter.reloadConfigs();
    }
  }

  public void checkAndReload() throws InvalidConfigException, LbAdapterExecuteException, IOException {
    adapter.checkConfigs();
    adapter.reloadConfigs();
  }

  public Optional<Collection<BaragonConfigFile>> configsToApply(ServiceContext context) throws MissingTemplateException {
    final BaragonService service = context.getService();
    final boolean previousConfigsExist = configsExist(service);
    Collection<BaragonConfigFile> newConfigs = configGenerator.generateConfigsForProject(context);
    if (previousConfigsExist && configsMatch(newConfigs, readConfigs(service))) {
      return Optional.absent();
    } else {
      return Optional.of(newConfigs);
    }
  }

  public boolean configsMatch(Collection<BaragonConfigFile> newConfigs, Collection<BaragonConfigFile> currentConfigs) {
    return currentConfigs.containsAll(newConfigs);
  }

  public void bootstrapApply(ServiceContext context, Collection<BaragonConfigFile> newConfigs) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException {
    final BaragonService service = context.getService();
    final boolean previousConfigsExist = configsExist(service);
    LOG.info(String.format("Going to apply %s: %s", service.getServiceId(), Joiner.on(", ").join(context.getUpstreams())));
    backupConfigs(service);
    try {
      writeConfigs(newConfigs);
      adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while writing configs for %s, reverting to backups!", service.getServiceId()), e);
      if (previousConfigsExist) {
        restoreConfigs(service);
      } else {
        remove(service, false);
      }
      throw Throwables.propagate(e);
    }
    LOG.info(String.format("Apply finished for %s", service.getServiceId()));
  }

  public void apply(ServiceContext context, Optional<BaragonService> maybeOldService, boolean revertOnFailure) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException {
    final BaragonService service = context.getService();
    final BaragonService oldService = maybeOldService.or(service);

    LOG.info(String.format("Going to apply %s: %s", service.getServiceId(), Joiner.on(", ").join(context.getUpstreams())));
    final boolean oldServiceExists = configsExist(oldService);
    final boolean previousConfigsExist = configsExist(service);

    Collection<BaragonConfigFile> newConfigs = configGenerator.generateConfigsForProject(context);

    if (configsMatch(newConfigs, readConfigs(oldService))) {
      LOG.info("    Configs are unchanged, skipping apply");
      return;
    }

    // Backup configs
    if (revertOnFailure) {
      backupConfigs(service);
      if (oldServiceExists) {
        backupConfigs(oldService);
      }
    }

    // Write & check the configs
    try {
      if (context.isPresent()) {
        writeConfigs(newConfigs);
        //If the new service id for this base path is different, remove the configs for the old service id
        if (oldServiceExists && !oldService.getServiceId().equals(service.getServiceId())) {
          remove(oldService, false);
        }
      } else {
        remove(service, false);
      }

      adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while writing configs for %s, reverting to backups!", service.getServiceId()), e);

      // Restore configs
      if (revertOnFailure) {
        if (oldServiceExists && !oldService.equals(service)) {
          restoreConfigs(oldService);
        }
        if (previousConfigsExist) {
          restoreConfigs(service);
        } else {
          remove(service, false);
        }
      }

      throw Throwables.propagate(e);
    }

    adapter.reloadConfigs();

    removeBackupConfigs(oldService);
    LOG.info(String.format("Apply finished for %s", service.getServiceId()));
  }

  public void delete(BaragonService service, Optional<BaragonService> maybeOldService) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException {
    final boolean oldServiceExists = (maybeOldService.isPresent() && configsExist(maybeOldService.get()));
    final boolean previousConfigsExist = configsExist(service);
     try {
      if (previousConfigsExist) {
        backupConfigs(service);
        remove(service, false);
      }
      if (oldServiceExists && !maybeOldService.get().equals(service)) {
        backupConfigs(maybeOldService.get());
        remove(maybeOldService.get(), false);
      }
       adapter.checkConfigs();
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while deleting configs for %s, reverting to backups!", service.getServiceId()), e);
      if (oldServiceExists && !maybeOldService.get().equals(service)) {
        restoreConfigs(maybeOldService.get());
      }
      if (previousConfigsExist) {
        restoreConfigs(service);
      } else {
        remove(service, false);
      }

      throw Throwables.propagate(e);
     }
    adapter.reloadConfigs();
  }

  private void writeConfigs(Collection<BaragonConfigFile> files) {
    for (BaragonConfigFile file : files) {
      try {
        Files.write(file.getContent().getBytes(Charsets.UTF_8), new File(file.getFullPath()));
      } catch (IOException e) {
        LOG.error(String.format("Failed writing %s", file.getFullPath()), e);
        throw new RuntimeException(String.format("Failed writing %s", file.getFullPath()), e);
      }
    }
  }

  private Collection<BaragonConfigFile> readConfigs(BaragonService service) {
    final Collection<BaragonConfigFile> configs = new ArrayList<>();

    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      try {
        configs.add(new BaragonConfigFile(filename, Files.asCharSource(file, Charsets.UTF_8).read()));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }

    return configs;
  }

  private void backupConfigs(BaragonService service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename);
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename + BACKUP_FILENAME_SUFFIX);
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error(String.format("Failed to backup %s", filename), e);
        throw new RuntimeException(String.format("Failed to backup %s", filename));
      }
    }
  }

  private void removeBackupConfigs(BaragonService service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename + BACKUP_FILENAME_SUFFIX);
      if (!file.exists()) {
        continue;
      }
    }
  }

  private boolean configsExist(BaragonService service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      if (!new File(filename).exists()) {
        return false;
      }
    }

    return true;
  }

  private void restoreConfigs(BaragonService service) {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      try {
        File src = new File(filename + BACKUP_FILENAME_SUFFIX);
        if (!src.exists()) {
          continue;
        }
        File dest = new File(filename);
        Files.copy(src, dest);
      } catch (IOException e) {
        LOG.error(String.format("Failed to restore %s", filename), e);
        throw new RuntimeException(String.format("Failed to restore %s", filename));
      }
    }
  }
}

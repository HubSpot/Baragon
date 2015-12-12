package com.hubspot.baragon.agent.lbs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;
import com.hubspot.baragon.exceptions.LockTimeoutException;
import com.hubspot.baragon.exceptions.MissingTemplateException;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FilesystemConfigHelper {
  public static final String BACKUP_FILENAME_SUFFIX = ".old";
  public static final String FAILED_CONFIG_SUFFIX = ".failed";

  private static final Logger LOG = LoggerFactory.getLogger(FilesystemConfigHelper.class);

  private final LbConfigGenerator configGenerator;
  private final LocalLbAdapter adapter;
  private final ReentrantLock agentLock;
  private final long agentLockTimeoutMs;
  private final BaragonAgentConfiguration configuration;

  @Inject
  public FilesystemConfigHelper(LbConfigGenerator configGenerator,
                                LocalLbAdapter adapter,
                                BaragonAgentConfiguration configuration,
                                @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock,
                                @Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long agentLockTimeoutMs) {
    this.configGenerator = configGenerator;
    this.adapter = adapter;
    this.configuration = configuration;
    this.agentLock = agentLock;
    this.agentLockTimeoutMs = agentLockTimeoutMs;
  }

  public void remove(BaragonService service) throws LbAdapterExecuteException, IOException {
    for (String filename : configGenerator.getConfigPathsForProject(service)) {
      File file = new File(filename);
      if (!file.exists()) {
        continue;
      }

      if (!file.delete()) {
        throw new RuntimeException(String.format("Failed to remove %s for %s", filename, service.getServiceId()));
      }
    }
  }

  public void checkAndReload() throws InvalidConfigException, LbAdapterExecuteException, IOException, InterruptedException, LockTimeoutException {
    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      throw new LockTimeoutException(String.format("Timed out waiting to acquire lock for reload"), agentLock);
    }

    try {
      adapter.checkConfigs();
      adapter.reloadConfigs();
    } catch (Exception e) {
      LOG.error("Caught exception while trying to reload configs", e);
      throw Throwables.propagate(e);
    } finally {
      agentLock.unlock();
    }
  }

  public Optional<Collection<BaragonConfigFile>> configsToApply(ServiceContext context) throws MissingTemplateException {
    final BaragonService service = context.getService();
    final boolean previousConfigsExist = configsExist(service);
    Collection<BaragonConfigFile> newConfigs = configGenerator.generateConfigsForProject(context, configuration.getExtraAgentData());
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
      saveAsFailed(service);
      if (previousConfigsExist) {
        restoreConfigs(service);
      } else {
        remove(service);
      }
      throw Throwables.propagate(e);
    }
    LOG.info(String.format("Apply finished for %s", service.getServiceId()));
  }

  public void apply(ServiceContext context, Optional<BaragonService> maybeOldService, boolean revertOnFailure, boolean noReload, boolean noValidate) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException, InterruptedException, LockTimeoutException {
    final BaragonService service = context.getService();
    final BaragonService oldService = maybeOldService.or(service);

    LOG.info(String.format("Going to apply %s: %s", service.getServiceId(), Joiner.on(", ").join(context.getUpstreams())));
    final boolean oldServiceExists = configsExist(oldService);
    final boolean previousConfigsExist = configsExist(service);

    Collection<BaragonConfigFile> newConfigs = configGenerator.generateConfigsForProject(context, configuration.getExtraAgentData());


    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      throw new LockTimeoutException("Timed out waiting to acquire lock", agentLock);
    }

    try {
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
      if (context.isPresent()) {
        writeConfigs(newConfigs);
        //If the new service id for this base path is different, remove the configs for the old service id
        if (oldServiceExists && !oldService.getServiceId().equals(service.getServiceId())) {
          remove(oldService);
        }
      } else {
        remove(service);
      }

      if (!noValidate) {
        adapter.checkConfigs();
      } else {
        LOG.debug("Not validating configs due to 'noValidate' specified in request");
      }
      if (!noReload) {
        adapter.reloadConfigs();
      } else {
        LOG.debug("Not reloading configs due to 'noReload' specified in request");
      }
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while writing configs for %s, reverting to backups!", service.getServiceId()), e);
      saveAsFailed(service);
      // Restore configs
      if (revertOnFailure) {
        if (oldServiceExists && !oldService.equals(service)) {
          restoreConfigs(oldService);
        }
        if (previousConfigsExist) {
          restoreConfigs(service);
        } else {
          remove(service);
        }
      }

      throw Throwables.propagate(e);
    } finally {
      agentLock.unlock();
    }

    removeBackupConfigs(oldService);
    LOG.info(String.format("Apply finished for %s", service.getServiceId()));
  }

  public void delete(BaragonService service, Optional<BaragonService> maybeOldService, boolean noReload, boolean noValidate) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException, InterruptedException, LockTimeoutException {
    final boolean oldServiceExists = (maybeOldService.isPresent() && configsExist(maybeOldService.get()));
    final boolean previousConfigsExist = configsExist(service);

    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      throw new LockTimeoutException("Timed out waiting to acquire lock for delete", agentLock);
    }
    try {
      if (previousConfigsExist) {
        backupConfigs(service);
        remove(service);
      }
      if (oldServiceExists && !maybeOldService.get().equals(service)) {
        backupConfigs(maybeOldService.get());
        remove(maybeOldService.get());
      }
      if (!noValidate) {
        adapter.checkConfigs();
      } else {
        LOG.debug("Not validating configs due to 'noValidate' specified in request");
      }
      if (!noReload) {
        adapter.reloadConfigs();
      } else {
        LOG.debug("Not reloading configs due to 'noReload' specified in request");
      }
    } catch (Exception e) {
      LOG.error(String.format("Caught exception while deleting configs for %s, reverting to backups!", service.getServiceId()), e);
      saveAsFailed(service);
      if (oldServiceExists && !maybeOldService.get().equals(service)) {
        restoreConfigs(maybeOldService.get());
      }
      if (previousConfigsExist) {
        restoreConfigs(service);
      } else {
        remove(service);
      }

      throw Throwables.propagate(e);
    } finally {
      agentLock.unlock();
    }
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
      if (new File(filename).exists()) {
        return true;
      }
    }

    return false;
  }

  private void saveAsFailed(BaragonService service) {
    if (configuration.isSaveFailedConfigs()) {
      for (String filename : configGenerator.getConfigPathsForProject(service)) {
        try {
          File src = new File(filename);
          if (!src.exists()) {
            continue;
          }
          File dest = new File(filename + FAILED_CONFIG_SUFFIX);
          Files.copy(src, dest);
        } catch (IOException e) {
          LOG.warn(String.format("Failed to save failed config %s", filename), e);
        }
      }
    }
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

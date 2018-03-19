package com.hubspot.baragon.agent.lbs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public void checkAndReload() throws Exception {
    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      LOG.warn("Failed to acquire lock for reload");
      throw new LockTimeoutException("Timed out waiting to acquire lock for reload", agentLock);
    }
    LOG.debug("Acquired agent lock, reloading configs");
    try {
      LOG.debug("Checking configs for reload");
      adapter.checkConfigs();
      LOG.debug("Reloading configs");
      adapter.reloadConfigs();
    }  finally {
      agentLock.unlock();
    }
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
    LOG.info("Going to apply {}: {}", service.getServiceId(), Joiner.on(", ").join(context.getUpstreams()));
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

  public void apply(ServiceContext context, Optional<BaragonService> maybeOldService, boolean revertOnFailure, boolean noReload, boolean noValidate, boolean delayReload, Optional<Integer> batchItemNumber) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException, InterruptedException, LockTimeoutException {
    final BaragonService service = context.getService();
    final BaragonService oldService = maybeOldService.or(service);

    LOG.info("Going to apply {}: {}", service.getServiceId(), Joiner.on(", ").join(context.getUpstreams()));
    final boolean oldServiceExists = configsExist(oldService);
    final boolean previousConfigsExist = configsExist(service);

    Collection<BaragonConfigFile> newConfigs = configGenerator.generateConfigsForProject(context);


    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      LockTimeoutException lte = new LockTimeoutException("Timed out waiting to acquire lock", agentLock);
      LOG.warn("Failed to acquire lock for service config apply ({})", service.getServiceId(), lte);
      throw lte;
    }

    LOG.debug("({}) Acquired agent lock, applying configs");

    try {
      if (configsMatch(newConfigs, readConfigs(oldService))) {
        LOG.info("({}) Configs are unchanged, skipping apply", service.getServiceId());
        if (!noReload && !delayReload && batchItemNumber.isPresent() && batchItemNumber.get() > 1) {
          LOG.debug("({}) Item is the last in a batch, reloading configs", service.getServiceId());
          adapter.reloadConfigs();
        }
        return;
      }

      // Backup configs
      LOG.debug("({}) Backing up configs", service.getServiceId());
      if (revertOnFailure) {
        backupConfigs(service);
        if (oldServiceExists) {
          backupConfigs(oldService);
        }
      }

      // Write & check the configs
      if (context.isPresent()) {
        LOG.debug("({}) Writing new configs", service.getServiceId());
        writeConfigs(newConfigs);
        //If the new service id for this base path is different, remove the configs for the old service id
        if (oldServiceExists && !oldService.getServiceId().equals(service.getServiceId())) {
          LOG.debug("({}) Removing old configs from renamed service", service.getServiceId());
          remove(oldService);
        }
      } else {
        LOG.debug("({}) Removing configs from deleted service", service.getServiceId());
        remove(service);
      }

      if (!noValidate) {
        LOG.debug("({}) Checking configs", service.getServiceId());
        adapter.checkConfigs();
      } else {
        LOG.debug("({}) Not validating configs due to 'noValidate' specified in request", service.getServiceId());
      }
      if (!noReload && !delayReload) {
        LOG.debug("({}) Reloading configs", service.getServiceId());
        adapter.reloadConfigs();
      } else {
        LOG.debug("({}) Not reloading configs: {}", service.getServiceId(), noReload ? "'noReload' specified in request" : "Will reload at end of request batch");
      }
    } catch (Exception e) {
      LOG.error("Caught exception while writing configs for {}, reverting to backups!", service.getServiceId(), e);
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

      throw new RuntimeException(e);
    } finally {
      agentLock.unlock();
    }

    removeBackupConfigs(oldService);
    LOG.info(String.format("Apply finished for %s", service.getServiceId()));
  }

  public void delete(BaragonService service, Optional<BaragonService> maybeOldService, boolean noReload, boolean noValidate, boolean delayReload) throws InvalidConfigException, LbAdapterExecuteException, IOException, MissingTemplateException, InterruptedException, LockTimeoutException {
    final boolean oldServiceExists = (maybeOldService.isPresent() && configsExist(maybeOldService.get()));
    final boolean previousConfigsExist = configsExist(service);

    if (!agentLock.tryLock(agentLockTimeoutMs, TimeUnit.MILLISECONDS)) {
      LOG.warn("Failed to acquire lock for service config delete ({})", service.getServiceId());
      throw new LockTimeoutException("Timed out waiting to acquire lock for delete", agentLock);
    }

    LOG.debug("Acquired agent lock, deleting configs");

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
      if (!noReload && !delayReload) {
        adapter.reloadConfigs();
      } else {
        LOG.debug("Not reloading configs: {}", noReload ? "'noReload' specified in request" : "Will reload at end of request batch");
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
        File  configFile = new File(file.getFullPath());
        if (configFile.getParentFile() != null) {
          if (!configFile.getParentFile().exists() && !configFile.getParentFile().mkdirs()) {
            throw new IOException(String.format("Could not create parent directories for file path %s", file.getFullPath()));
          }
        }
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
        Files.move(src, dest);
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

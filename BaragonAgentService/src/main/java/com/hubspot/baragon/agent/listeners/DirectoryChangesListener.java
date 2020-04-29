package com.hubspot.baragon.agent.listeners;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.config.WatchedDirectoryConfig;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.exceptions.LockTimeoutException;

@Singleton
public class DirectoryChangesListener {
  private static final Logger LOG = LoggerFactory.getLogger(DirectoryChangesListener.class);

  private final BaragonAgentConfiguration configuration;
  private final FilesystemConfigHelper filesystemConfigHelper;
  private final ReentrantLock agentLock;
  private final ExecutorService executorService;
  private final AtomicReference<String> fileCopyErrorMessage;

  private Future<?> future;

  @Inject
  public DirectoryChangesListener(BaragonAgentConfiguration configuration,
                                  FilesystemConfigHelper filesystemConfigHelper,
                                  @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock) {
    this.configuration = configuration;
    this.filesystemConfigHelper = filesystemConfigHelper;
    this.agentLock = agentLock;
    this.fileCopyErrorMessage = new AtomicReference<>(null);
    this.executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("directory-watcher-%d").build());
  }

  public void start() throws Exception {
    if (!configuration.getWatchedDirectories().isEmpty()) {
      for (WatchedDirectoryConfig config : configuration.getWatchedDirectories()) {
        // initial setup
        handleFileChangeForDirectory(config);
      }
      future = executorService.submit(() -> this.watchDirectories(configuration.getWatchedDirectories()));
    }
  }

  public void stop() {
    if (future != null) {
      future.cancel(true);
    }
    executorService.shutdown();
  }

  private void watchDirectories(List<WatchedDirectoryConfig> directoryConfigs) {
    while (!Thread.interrupted()) {
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
        Map<WatchKey, WatchedDirectoryConfig> watchKeyToDirectory = new HashMap<>();
        for (WatchedDirectoryConfig d : directoryConfigs) {
          LOG.info("Watching directory {} to copy to {}", d.getSource(), d.getDestination());
          Path path = d.getSourceAsPath();
          watchKeyToDirectory.put(path.register(watchService), d);
        }
        while (!Thread.interrupted()) {
          WatchKey key = watchService.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind().equals(StandardWatchEventKinds.OVERFLOW)) {
              LOG.warn("Overflow event received, stopping file watch");
              return;
            }
            WatchedDirectoryConfig config = watchKeyToDirectory.get(key);
            handleFileChangeForDirectory(config);
          }
          boolean valid = key.reset();
          if (!valid) {
            LOG.warn("Key for {} is not accessible, stopping watch", watchKeyToDirectory.get(key));
          }
        }
      } catch (InterruptedException ie) {
        LOG.warn("Interrupted, shutting down");
        return;
      } catch (Exception e) {
        fileCopyErrorMessage.set(e.getMessage());
        LOG.error("Unexpected exception while watching directories", e);
      }
      try {
        LOG.warn("Watcher unexpectedly exited, sleeping and restarting");
        Thread.sleep(30000);
      } catch (InterruptedException ie) {
        LOG.warn("Interrupted, shutting down");
        return;
      }
    }
  }

  public void handleFileChangeForDirectory(WatchedDirectoryConfig config) throws Exception {
    if (!agentLock.tryLock(45, TimeUnit.SECONDS)) {
      LOG.warn("Failed to acquire lock for reload");
      throw new LockTimeoutException("Timed out waiting to acquire lock for reload", agentLock);
    }
    try {
      List<Path> destinationFiles = getFilesInDirectory(config.getDestinationAsPath());
      for (Path path : destinationFiles) {
        filesystemConfigHelper.backupFile(path.toAbsolutePath().toString());
      }
      LOG.info("Backed up {} files in {}", destinationFiles.size(), config.getDestination());
      try {
        LOG.info("Copying files from {} to {}", config.getSource(), config.getDestination());
        List<Path> toCopy = getFilesInDirectory(config.getSourceAsPath());
        for (Path from : toCopy) {
          Path to = Paths.get(from.toAbsolutePath().toString().replace(config.getSource(), config.getDestination()));
          Files.copy(from, to);
        }
        LOG.info("Copied {} files to {}", toCopy.size(), config.getDestination());
        filesystemConfigHelper.checkAndReloadUnlocked();
        LOG.info("File copy succeeded for {}", config);
        fileCopyErrorMessage.set(null);
      } catch (Exception e) {
        fileCopyErrorMessage.set(e.getMessage());
        List<Path> toCleanUp = getFilesInDirectory(config.getDestinationAsPath());
        for (Path path : toCleanUp) {
          Path absolute = path.toAbsolutePath();
          if (filesystemConfigHelper.isBackupFile(absolute.toString())) {
            Files.delete(absolute);
          } else {
            filesystemConfigHelper.restoreFile(absolute.toString());
          }
        }
        LOG.info("Cleaned up {} files in {} due to exception", toCleanUp.size(), config.getDestination());
        throw e;
      }
    } finally {
      agentLock.unlock();
    }
  }

  private List<Path> getFilesInDirectory(Path directory) throws IOException {
    try (Stream<Path> walk = Files.walk(directory)) {
      return walk.filter(Files::isRegularFile)
          .collect(Collectors.toList());
    }
  }

  public Optional<String> getErrorMessage() {
    return Optional.fromNullable(fileCopyErrorMessage.get());
  }
}

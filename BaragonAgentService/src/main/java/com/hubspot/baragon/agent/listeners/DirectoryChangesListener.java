package com.hubspot.baragon.agent.listeners;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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

@Singleton
public class DirectoryChangesListener {
  private static final Logger LOG = LoggerFactory.getLogger(
    DirectoryChangesListener.class
  );

  private final BaragonAgentConfiguration configuration;
  private final FilesystemConfigHelper filesystemConfigHelper;
  private final ReentrantLock agentLock;
  private final ExecutorService executorService;
  private final AtomicReference<String> fileCopyErrorMessage;
  private final Timer timer;
  private final Set<WatchedDirectoryConfig> pendingUpdates;

  private Future<?> future;

  @Inject
  public DirectoryChangesListener(
    BaragonAgentConfiguration configuration,
    FilesystemConfigHelper filesystemConfigHelper,
    @Named(BaragonAgentServiceModule.AGENT_LOCK) ReentrantLock agentLock
  ) {
    this.configuration = configuration;
    this.filesystemConfigHelper = filesystemConfigHelper;
    this.agentLock = agentLock;
    this.fileCopyErrorMessage = new AtomicReference<>(null);
    this.timer = new Timer();
    this.pendingUpdates = new HashSet<>();
    this.executorService =
      Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("directory-watcher-%d").build()
      );
  }

  public void start() throws Exception {
    if (!configuration.getWatchedDirectories().isEmpty()) {
      for (WatchedDirectoryConfig config : configuration.getWatchedDirectories()) {
        // initial setup, should throw an exception that blocks startup for invalid config
        handleFileChangeForDirectory(config);
      }
      future =
        executorService.submit(
          () -> this.watchDirectories(configuration.getWatchedDirectories())
        );
    }
  }

  public void stop() {
    if (future != null) {
      future.cancel(true);
    }
    timer.cancel();
    executorService.shutdown();
  }

  private synchronized boolean addPending(WatchedDirectoryConfig config) {
    return pendingUpdates.add(config);
  }

  private synchronized void removePending(WatchedDirectoryConfig config) {
    pendingUpdates.remove(config);
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // Bug in spotbugs for try-with-resources
  private void watchDirectories(List<WatchedDirectoryConfig> directoryConfigs) {
    while (!Thread.interrupted()) {
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
        Map<WatchKey, WatchedDirectoryConfig> watchKeyToDirectory = new HashMap<>();
        for (WatchedDirectoryConfig d : directoryConfigs) {
          LOG.info(
            "Watching directory {} to copy to {}",
            d.getSource(),
            d.getDestination()
          );
          Path path = d.getSourceAsPath();
          watchKeyToDirectory.put(
            path.register(
              watchService,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_DELETE,
              StandardWatchEventKinds.ENTRY_MODIFY
            ),
            d
          );
        }
        while (!Thread.interrupted()) {
          WatchKey key = watchService.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind().equals(StandardWatchEventKinds.OVERFLOW)) {
              LOG.warn("Overflow event received, stopping file watch");
              return;
            }
            WatchedDirectoryConfig config = watchKeyToDirectory.get(key);
            if (addPending(config)) {
              timer.schedule(
                new TimerTask() {

                  @Override
                  public void run() {
                    try {
                      handleFileChangeForDirectory(config);
                    } catch (Exception e) {
                      LOG.error("Could not run file update for {}", config, e);
                    }
                  }
                },
                10000
              ); // Small bit of delay to debounce in case multiple updates were made at once
            }
          }
          boolean valid = key.reset();
          if (!valid) {
            LOG.warn(
              "Key for {} is not accessible, stopping watch",
              watchKeyToDirectory.get(key)
            );
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

  public void handleFileChangeForDirectory(WatchedDirectoryConfig config)
    throws Exception {
    try {
      if (!agentLock.tryLock(45, TimeUnit.SECONDS)) {
        LOG.warn("Failed to acquire lock for reload");
        throw new LockTimeoutException(
          "Timed out waiting to acquire lock for reload",
          agentLock
        );
      }
      List<Path> backedUpFiles = new ArrayList<>();
      List<Path> newCopiedFiles = new ArrayList<>();
      try {
        List<Path> destinationFiles = getFilesInDirectory(config.getDestinationAsPath());
        for (Path path : destinationFiles) {
          LOG.info("Backing up {}", path);
          filesystemConfigHelper.backupFile(path.toAbsolutePath().toString());
          backedUpFiles.add(path);
        }
        LOG.info(
          "Backed up {} files in {}",
          destinationFiles.size(),
          config.getDestination()
        );
        LOG.info(
          "Files in destination dir are now {}",
          getFilesInDirectory(config.getDestinationAsPath())
        );
        try {
          LOG.info(
            "Copying files from {} to {}",
            config.getSource(),
            config.getDestination()
          );
          List<Path> toCopy = getFilesInDirectory(config.getSourceAsPath());
          for (Path from : toCopy) {
            Path to = config
              .getDestinationAsPath()
              .resolve(from.getFileName().toString());
            LOG.info("Copying {} to {}", from, to);
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            newCopiedFiles.add(to);
          }
          LOG.info("Copied {} files to {}", toCopy.size(), config.getDestination());
          LOG.info(
            "Files in destination dir are now {}",
            getFilesInDirectory(config.getDestinationAsPath())
          );
          filesystemConfigHelper.checkAndReloadUnlocked();
          LOG.info("File copy succeeded for {}", config);
          fileCopyErrorMessage.set(null);
        } catch (Exception e) {
          fileCopyErrorMessage.set(e.getMessage());
          for (Path path : newCopiedFiles) {
            if (Files.exists(path)) {
              LOG.info("Deleting possibly invalid file {}", path);
              Files.delete(path);
            }
          }
          for (Path path : backedUpFiles) {
            LOG.info("Restoring file {}", path);
            filesystemConfigHelper.restoreFile(path.toString());
          }
          LOG.info(
            "Files in destination dir are now {}",
            getFilesInDirectory(config.getDestinationAsPath())
          );
          throw e;
        }
      } finally {
        agentLock.unlock();
      }
    } finally {
      removePending(config);
    }
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // Bug in spotbugs for try-with-resources
  private List<Path> getFilesInDirectory(Path directory) throws IOException {
    try (Stream<Path> walk = Files.walk(directory)) {
      return walk.filter(Files::isRegularFile).collect(Collectors.toList());
    }
  }

  public Optional<String> getErrorMessage() {
    return Optional.fromNullable(fileCopyErrorMessage.get());
  }
}

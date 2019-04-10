package com.hubspot.baragon.service.hollow.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;

public class AsyncBlobStorageCleaner extends BlobStorageCleaner {
  private static final Logger LOG = LoggerFactory.getLogger(AsyncBlobStorageCleaner.class);
  private static final int VERSION_OFFSET = 20;
  private static final int CLEAN_FREQUENCY = VERSION_OFFSET * 2;
  private static final int QUEUE_SIZE = CLEAN_FREQUENCY * 4;

  private final DataSource dataSource;
  private final NamingStrategy namingStrategy;
  private final Supplier<Long> currentVersionSupplier;
  private final BlockingQueue<Long> deleteQueue;
  private final AtomicLong enqueueCounter;
  private final AtomicLong cleanupTime;
  private final IndexStrategy indexStrategy;
  private final AtomicLong deletedUpTo;

  public AsyncBlobStorageCleaner(DataSource dataSource,
                                 NamingStrategy namingStrategy,
                                 Supplier<Long> currentVersionSupplier,
                                 IndexStrategy indexStrategy) {
    this.dataSource = dataSource;
    this.namingStrategy = namingStrategy;
    this.currentVersionSupplier = currentVersionSupplier;
    this.deleteQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    this.enqueueCounter = new AtomicLong(0);
    this.cleanupTime = new AtomicLong(0);
    this.indexStrategy = indexStrategy;
    this.deletedUpTo = new AtomicLong(0);

    Metrics.newGauge(AsyncBlobStorageCleaner.class, "cleanup-time", new Gauge<Long>() {
      @Override
      public Long value() {
        return cleanupTime.get();
      }
    });

    ExecutorService executor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setNameFormat("async-blob-storage-cleaner-%d")
            .setDaemon(true)
            .build());

    executor.submit(this::processDeleteQueue);
  }

  @Override
  public void cleanSnapshots() {
    enqueue();
  }

  @Override
  public void cleanDeltas() {
    enqueue();
  }

  @Override
  public void cleanReverseDeltas() {
    enqueue();
  }

  private void enqueue() {
    long counter = enqueueCounter.incrementAndGet();
    if (counter % CLEAN_FREQUENCY != 0) {
      LOG.debug("Skipping clean request {}", counter);
      return;
    }

    long version = currentVersionSupplier.get() - VERSION_OFFSET;
    LOG.debug("Processing delete enqueue request for version: {}", version);
    try {
      if (!deleteQueue.offer(version, 10, TimeUnit.MILLISECONDS)) {
        LOG.warn("Failed to enqueue version {} for deletion", version);
      }
    } catch (Exception e) {
      LOG.error("Encountered exception enqueueing version {} for deletion", version);
    }
  }

  private void processDeleteQueue() {
    while (true) {
      try {
        long initial = deleteQueue.take();
        List<Long> toDelete = new ArrayList<>();
        toDelete.add(initial);

        deleteQueue.drainTo(toDelete);

        long start = toDelete.stream()
            .mapToLong(Long::longValue)
            .max()
            .getAsLong();

        // ensure there is always a snapshot
        Optional<Long> snapshotVersion = indexStrategy.getClosestSnapshot(start);
        if (!snapshotVersion.isPresent()) {
          LOG.debug("Skipping delete as there is no snapshot available on or before version: {}", start);
          return;
        }

        deleteDataPriorTo(snapshotVersion.get());
        // this is necessary because of s3/gcs list caching
        deletedUpTo.set(snapshotVersion.get());
      } catch (InterruptedException e) {
        LOG.info("Interrupted, shutting down delete queue executor");
        return;
      } catch (Exception e) {
        LOG.error("Encountered exception processing delete queue", e);
      }
    }
  }

  private void deleteDataPriorTo(long version) {
    long cur = System.currentTimeMillis();
    LOG.debug("Processing snapshots prior to version: {}", version);
    indexStrategy.removeSnapshotsPriorTo(version);
    deleteForPrefixAndVersion(namingStrategy.getSnapshotPrefix(), version);

    LOG.debug("Processing deltas prior to version: {}", version);
    deleteForPrefixAndVersion(namingStrategy.getDeltaPrefix(), version);

    LOG.debug("Processing reverse deltas prior to version: {}", version);
    deleteForPrefixAndVersion(namingStrategy.getReverseDeltaPrefix(), version);

    cleanupTime.set(System.currentTimeMillis() - cur);
  }

  private void deleteForPrefixAndVersion(String prefix, long version) {
    StreamSupport.stream(dataSource.list(prefix).spliterator(), false)
        .filter(s -> shouldDelete(s, version))
        .forEach(this::delete);
  }

  private boolean shouldDelete(String name, long maxVersion) {
    Optional<Long> version = namingStrategy.getVersion(name);
    if (!version.isPresent()) {
      LOG.debug("File {} is not versioned, skipping", name);
      return false;
    }

    return version.get() >= deletedUpTo.get()
        && version.get() < maxVersion;
  }

  private void delete(String name) {
    LOG.debug("Deleting: {}", name);
    try {
      dataSource.delete(name);
    } catch (Exception e) {
      LOG.error("Could not delete {} as part of cleanup", name);
    }
  }
}

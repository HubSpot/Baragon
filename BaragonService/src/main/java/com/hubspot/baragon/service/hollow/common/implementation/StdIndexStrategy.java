package com.hubspot.baragon.service.hollow.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.hubspot.baragon.service.hollow.common.implementation.BaseIndexStrategy;
import com.netflix.hollow.core.memory.encoding.VarInt;

public class StdIndexStrategy extends BaseIndexStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(StdIndexStrategy.class);

  private final Lock lock;
  private List<Long> snapshotIndex;

  public StdIndexStrategy(DataSource source,
                          NamingStrategy namingStrategy) {
    super(source, namingStrategy);
    this.lock = new ReentrantLock();
    this.snapshotIndex = initializeSnapshotIndex();
  }

  @Override
  public void addSnapshotVersionToIndex(long version) {
    lock.lock();
    try {
      // insert the new version into the list
      int idx = Collections.binarySearch(snapshotIndex, version);
      int insertionPoint = Math.abs(idx) - 1;
      snapshotIndex.add(insertionPoint, version);

      // build a binary representation of the list -- gap encoded variable-length integers
      byte[] idxBytes = buildGapEncodedVarIntSnapshotIndex();
      getDataSource().set(getNamingStrategy().getIndexName(), ByteSource.wrap(idxBytes));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removeSnapshotsPriorTo(long version) {
    try {
      if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
        LOG.error("Failed to acquire lock for cleanup");
        return;
      }

      try {
        snapshotIndex.removeIf(i -> i < version);
      } finally {
        lock.unlock();
      }
    } catch (InterruptedException e) {
      LOG.debug("Snapshot removal interrupted, skipping");
    }
  }

  private byte[] buildGapEncodedVarIntSnapshotIndex() {
    int idx;
    byte[] idxBytes;
    idx = 0;
    long currentSnapshotId = snapshotIndex.get(idx++);
    long currentSnapshotIdGap = currentSnapshotId;
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      while (idx < snapshotIndex.size()) {
        VarInt.writeVLong(os, currentSnapshotIdGap);

        long nextSnapshotId = snapshotIndex.get(idx++);
        currentSnapshotIdGap = nextSnapshotId - currentSnapshotId;
        currentSnapshotId = nextSnapshotId;
      }

      VarInt.writeVLong(os, currentSnapshotIdGap);

      idxBytes = os.toByteArray();
    } catch (IOException shouldNotHappen) {
      throw new RuntimeException(shouldNotHappen);
    }

    return idxBytes;
  }

  private List<Long> initializeSnapshotIndex() {
    List<Long> snapshotIdx = new ArrayList<>();
    LOG.debug("Initializing index for: {}", getNamingStrategy().getSnapshotPrefix());
    for (String name : getDataSource().list(getNamingStrategy().getSnapshotPrefix())) {
      if (getNamingStrategy().isSnapshot(name)) {
        addSnapshotStateId(name, snapshotIdx);
      }
    }

    Collections.sort(snapshotIdx);
    return snapshotIdx;
  }

  private void addSnapshotStateId(String name, List<Long> snapshotIdx) {
    Optional<Long> version = getNamingStrategy().getVersion(name);
    if (!version.isPresent()) {
      LOG.error("Could not determine version for indexing file: {}", name);
      return;
    }

    LOG.debug("Adding version: {} to {} index", getNamingStrategy().getSnapshotPrefix(), version.get());
    snapshotIdx.add(version.get());
  }
}

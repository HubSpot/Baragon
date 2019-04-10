package com.hubspot.baragon.service.hollow.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.google.common.io.ByteSource;
import com.netflix.hollow.core.memory.encoding.VarInt;

public abstract class BaseIndexStrategy implements IndexStrategy {
  private final DataSource dataSource;
  private final NamingStrategy namingStrategy;

  public BaseIndexStrategy(DataSource dataSource,
                           NamingStrategy namingStrategy) {
    this.dataSource = dataSource;
    this.namingStrategy = namingStrategy;
  }

  @Override
  public Optional<Long> getClosestSnapshot(long version) {
    ByteSource source = dataSource.getExpected(namingStrategy.getIndexName());
    try (InputStream stream = source.openBufferedStream()) {
      return computeClosestSnapshotId(version, stream, source.size());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read indexFile!");
    }
  }

  protected DataSource getDataSource() {
    return dataSource;
  }

  protected NamingStrategy getNamingStrategy() {
    return namingStrategy;
  }

  private Optional<Long> computeClosestSnapshotId(long desiredVersion, InputStream stream, long streamLength) {
    long pos = 0;
    long currentSnapshotStateId = 0;

    // There was no exact match for a snapshot leading to the desired state.
    // use the index to find the nearest one before the desired state.
    while (pos < streamLength) {
      long nextGap = readGap(stream);

      if (currentSnapshotStateId + nextGap > desiredVersion) {
        if (currentSnapshotStateId == 0) {
          return Optional.empty();
        }

        return Optional.of(currentSnapshotStateId);
      }

      currentSnapshotStateId += nextGap;
      pos += VarInt.sizeOfVLong(nextGap);
    }

    if (currentSnapshotStateId != 0 || pos > 0) {
      return Optional.of(currentSnapshotStateId);
    }

    return Optional.empty();
  }

  private long readGap(InputStream stream) {
    try {
      return VarInt.readVLong(stream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read index gap");
    }
  }
}

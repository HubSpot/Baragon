package com.hubspot.baragon.service.hollow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.Metadata;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.consumer.HollowConsumer.Blob;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;

public class HollowDataReplicationBlobRetriever implements BlobRetriever {
  private static final Logger LOG = LoggerFactory.getLogger(HollowDataReplicationBlobRetriever.class);
  private final DataSource source;
  private final NamingStrategy namingStrategy;
  private final IndexStrategy indexStrategy;

  public HollowDataReplicationBlobRetriever(DataSource source,
                                            NamingStrategy namingStrategy,
                                            IndexStrategy indexStrategy) {
    this.source = source;
    this.namingStrategy = namingStrategy;
    this.indexStrategy = indexStrategy;
  }

  @Override
  public Blob retrieveSnapshotBlob(long desiredVersion) {
    LOG.debug("Attempting to retrieve snapshot blob for version: {}", desiredVersion);
    Optional<Blob> result = fetchBlob(namingStrategy.getSnapshotName(desiredVersion));
    while (!result.isPresent()) {
      LOG.debug("Snapshot for version {} not available, retrieving latest", desiredVersion);
      Optional<Long> version = indexStrategy.getClosestSnapshot(desiredVersion);
      if (!version.isPresent()) {
        LOG.debug("Could not find snapshot version closest to version: {}", version);
        return null;
      }

      result = fetchBlob(namingStrategy.getSnapshotName(version.get()))
          .map(this::validateIsSnapshot);

      desiredVersion = version.get() == desiredVersion
          ? desiredVersion - 1
          : version.get();
    }

    return result.map(this::validateIsSnapshot).orElse(null);
  }

  @Override
  public Blob retrieveDeltaBlob(long currentVersion) {
    LOG.debug("Attempting to retrieve delta blob for version: {}", currentVersion);
    return fetchBlob(namingStrategy.getDeltaName(currentVersion))
        .map(this::validateIsDelta)
        .orElse(null);
  }

  @Override
  public Blob retrieveReverseDeltaBlob(long currentVersion) {
    LOG.debug("Attempting to retrieve reverse delta blob for version: {}", currentVersion);
    return fetchBlob(namingStrategy.getReverseDeltaName(currentVersion))
        .map(this::validateIsReverseDelta)
        .orElse(null);
  }

  private Optional<Blob> fetchBlob(String objectName) {
    return source.getMetadata(objectName)
        .map(m -> new ByteSourceBlob(m, source.getExpected(objectName)));
  }

  private Blob validateIsDelta(Blob blob) {
    if (!blob.isDelta()) {
      throw new IllegalArgumentException("Provided blob is not a delta!");
    }

    return blob;
  }

  private Blob validateIsReverseDelta(Blob blob) {
    if (!blob.isReverseDelta()) {
      throw new IllegalArgumentException("Provided blob is not a reverse delta!");
    }

    return blob;
  }

  private Blob validateIsSnapshot(Blob blob) {
    if (!blob.isSnapshot()) {
      throw new IllegalArgumentException("Provided blob is not a snapshot!");
    }

    return blob;
  }

  private static class ByteSourceBlob extends Blob {
    private final ByteSource source;

    ByteSourceBlob(Metadata metadata, ByteSource source) {
      super(metadata.getFromVersionMaybe().orElse(Long.MIN_VALUE), metadata.getToVersion());
      this.source = source;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return source.openBufferedStream();
    }
  }
}

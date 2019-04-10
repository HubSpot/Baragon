package com.hubspot.baragon.service.hollow.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.Metadata;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.producer.HollowProducer.Blob;
import com.netflix.hollow.api.producer.HollowProducer.Publisher;

public class HollowDataReplicationPublisher implements Publisher {
  private static final Logger LOG = LoggerFactory.getLogger(HollowDataReplicationPublisher.class);

  private final DataSource source;
  private final NamingStrategy namingStrategy;
  private final IndexStrategy indexStrategy;

  public HollowDataReplicationPublisher(DataSource source,
                                        NamingStrategy namingStrategy,
                                        IndexStrategy indexStrategy) {
    this.source = source;
    this.namingStrategy = namingStrategy;
    this.indexStrategy = indexStrategy;
  }

  @Override
  public void publish(Blob blob) {
    switch (blob.getType()) {
      case SNAPSHOT:
        publishSnapshot(blob);
        break;
      case DELTA:
        publishDelta(blob);
        break;
      case REVERSE_DELTA:
        publishReverseDelta(blob);
        break;
    }
  }

  public void publishSnapshot(Blob blob) {
    String objectName = namingStrategy.getSnapshotName(blob.getToVersion());
    LOG.debug("Publishing Snapshot: {}", objectName);
    source.set(Metadata.from(objectName, blob), toByteSource(blob));
    indexStrategy.addSnapshotVersionToIndex(blob.getToVersion());
  }

  public void publishDelta(Blob blob) {
    String objectName = namingStrategy.getDeltaName(blob.getFromVersion());
    LOG.debug("Publishing Delta: {}", objectName);
    source.set(Metadata.from(objectName, blob), toByteSource(blob));
  }

  public void publishReverseDelta(Blob blob) {
    String objectName = namingStrategy.getReverseDeltaName(blob.getFromVersion());
    LOG.debug("Publishing Reverse Delta: {}", objectName);
    source.set(Metadata.from(objectName, blob), toByteSource(blob));
  }

  private ByteSource toByteSource(Blob blob) {
    try {
      return ByteSource.wrap(ByteStreams.toByteArray(blob.newInputStream()));
    } catch (Exception e) {
      throw new RuntimeException("Failed to read blob data", e);
    }
  }
}

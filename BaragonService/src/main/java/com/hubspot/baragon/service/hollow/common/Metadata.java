package com.hubspot.baragon.service.hollow.common;

import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.netflix.hollow.api.producer.HollowProducer.Blob;

public class Metadata {
  private final String blobName;
  private final long toVersion;
  private final Optional<Long> fromVersion;
  private final long timestamp;

  public static Metadata from(String name, Blob blob) {
    return new Metadata(
        name,
        blob.getToVersion(),
        Optional.of(blob.getFromVersion()).filter(v -> v > Long.MIN_VALUE),
        Optional.empty());
  }

  public Metadata(String blobName,
                  long toVersion,
                  Optional<Long> fromVersion,
                  Optional<Long> timestamp) {
    this.blobName = blobName;
    this.toVersion = toVersion;
    this.fromVersion = fromVersion;
    this.timestamp = timestamp.orElse(System.currentTimeMillis());
  }

  public String getBlobName() {
    return blobName;
  }

  public long getToVersion() {
    return toVersion;
  }

  public Optional<Long> getFromVersionMaybe() {
    return fromVersion;
  }

  public long getFromVersion() {
    return getFromVersionMaybe()
        .orElseThrow(() -> new IllegalStateException("Metadata for blob " + blobName + " does not have a from version tag"));
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        blobName,
        toVersion,
        fromVersion.orElse(null));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Metadata)) {
      return false;
    }

    Metadata that = (Metadata) obj;
    return that.blobName.endsWith(this.blobName)
        && that.toVersion == this.toVersion
        && that.fromVersion.equals(this.fromVersion);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Metadata.class)
        .add("blobName", blobName)
        .add("toVersion", toVersion)
        .add("fromVersion", fromVersion.orElse(null))
        .toString();
  }


}

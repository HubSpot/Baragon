package com.hubspot.baragon.service.hollow.producer;

import java.util.Optional;

import com.google.common.io.ByteSource;
import com.google.common.primitives.Longs;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;

public class RestoringWatcher {
  private final long version;

  RestoringWatcher(DataSource dataSource, NamingStrategy namingStrategy) {
    this.version = loadVersion(dataSource, namingStrategy);
  }

  public boolean hasLatestVersion() {
    return version != AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE;
  }

  public long getLatestVersion() {
    return version;
  }

  private long loadVersion(DataSource dataSource, NamingStrategy namingStrategy) {
    try {
      Optional<ByteSource> source = dataSource.get(namingStrategy.getAnnouncedVersionName());
      if (source.isPresent()) {
        return Longs.fromByteArray(source.get().read());
      }

      return AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load version!", e);
    }
  }
}

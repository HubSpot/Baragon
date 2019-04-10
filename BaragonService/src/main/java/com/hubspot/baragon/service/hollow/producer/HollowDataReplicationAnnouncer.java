package com.hubspot.baragon.service.hollow.producer;

import com.google.common.io.ByteSource;
import com.google.common.primitives.Longs;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.producer.HollowProducer.Announcer;

public class HollowDataReplicationAnnouncer implements Announcer {
  private final DataSource dataSource;
  private final NamingStrategy namingStrategy;

  public HollowDataReplicationAnnouncer(DataSource dataSource,
                                        NamingStrategy namingStrategy) {
    this.dataSource = dataSource;
    this.namingStrategy = namingStrategy;
  }

  @Override
  public void announce(long stateVersion) {
    dataSource.set(namingStrategy.getAnnouncedVersionName(), ByteSource.wrap(Longs.toByteArray(stateVersion)));
  }
}

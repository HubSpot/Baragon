package com.hubspot.baragon.service.hollow.common;

import java.util.Optional;

public interface NamingStrategy {
  String getIndexName();
  String getAnnouncedVersionName();
  String getSnapshotName(long version);
  String getDeltaName(long version);
  String getReverseDeltaName(long version);

  String getSnapshotPrefix();
  String getDeltaPrefix();
  String getReverseDeltaPrefix();

  boolean isSnapshot(String name);
  boolean isDelta(String name);
  boolean isReverseDelta(String name);

  Optional<Long> getVersion(String name);
}

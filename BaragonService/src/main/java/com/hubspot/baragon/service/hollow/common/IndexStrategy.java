package com.hubspot.baragon.service.hollow.common;

import java.util.Optional;

public interface IndexStrategy {
  Optional<Long> getClosestSnapshot(long version);
  void addSnapshotVersionToIndex(long version);
  void removeSnapshotsPriorTo(long version);

}

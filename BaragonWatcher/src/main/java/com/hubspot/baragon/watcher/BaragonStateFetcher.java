package com.hubspot.baragon.watcher;

import com.google.inject.ImplementedBy;
import com.hubspot.baragon.models.BaragonServiceState;

import java.util.Collection;

@ImplementedBy(DefaultBaragonStateFetcher.class)
public interface BaragonStateFetcher {
  Collection<BaragonServiceState> fetchState(int version);
}

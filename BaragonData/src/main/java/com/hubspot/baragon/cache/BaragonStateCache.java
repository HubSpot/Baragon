package com.hubspot.baragon.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonStateDatastore;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class BaragonStateCache {
  private final BaragonStateDatastore stateDatastore;
  private final AtomicReference<CachedBaragonState> currentState;

  @Inject
  public BaragonStateCache(BaragonStateDatastore stateDatastore) {
    this.stateDatastore = stateDatastore;
    this.currentState = new AtomicReference<>(new CachedBaragonState(new byte[0], -2));
  }

  public CachedBaragonState getState(boolean refresh) {
    CachedBaragonState previousState = currentState.get();
    int version = stateDatastore.getStateVersion().or(-1);

    if (!refresh && previousState.getVersion() == version) {
      return previousState;
    } else {
      return updateState(version, refresh);
    }
  }

  private synchronized CachedBaragonState updateState(int version, boolean refresh) {
    CachedBaragonState previousState = currentState.get();

    if (!refresh && previousState.getVersion() >= version) {
      return previousState;
    } else {
      CachedBaragonState newState = fetchState(version, refresh);
      currentState.set(newState);
      return newState;
    }
  }

  private CachedBaragonState fetchState(int version, boolean refresh) {
    return new CachedBaragonState(stateDatastore.getGlobalStateAsBytes(refresh), version);
  }
}
